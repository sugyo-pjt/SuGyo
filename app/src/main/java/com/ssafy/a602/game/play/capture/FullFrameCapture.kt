package com.ssafy.a602.game.play.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.ssafy.a602.game.play.dto.Coordinate
import com.ssafy.a602.game.play.dto.FrameBlock
import com.ssafy.a602.game.play.dto.PoseBlock
import com.ssafy.a602.game.play.dto.SimilarityRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min

/**
 * 1) 목표: "노래가 재생되는 동안 들어온 모든 카메라 프레임"을 100% 보존하고
 *    후단에서 MediaPipe Pose/Hand를 VIDEO 모드로 프레임-바이-프레임 처리.
 *
 * 2) 구조:
 *    - CameraX(ImageAnalysis): YUV_420_888 → NV21 '빠른 복사'만 수행, 즉시 image.close()
 *    - frameQueue: 메모리 큐(가득 차면 파일 스풀로 우회)
 *    - MediaPipe 워커: 큐/파일에서 순차 꺼내 VIDEO 모드로 detectForVideo() 수행
 */

// ---------- 데이터 & 큐 ----------

data class RawFrame(
    val tsMs: Long,
    val width: Int,
    val height: Int,
    val rotationDeg: Int,
    val nv21: ByteArray
)

private const val TAG = "FullFrameCapture"

// 100% 보존 우선. 메모리 부족 대비 넉넉히(필요 시 조정)
private const val QUEUE_CAPACITY = 512

// ---------- 파일 스풀러 (선택) ----------

/**
 * 프레임이 메모리 큐를 초과할 때 임시 파일로 흘려보냄.
 * 포맷: [MAGIC(4B) 'SRKS'][VER(1B)=1][COUNT(8B, 옵션)]
 * 이후 프레임 반복: [tsMs(8)][w(4)][h(4)][rot(4)][len(4)][NV21 bytes...]
 */
class FrameSpooler(context: Context) : Closeable {
    private val file = File.createTempFile("srks_frames_", ".bin", context.cacheDir)
    private val dos = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
    private var count = 0L

    init {
        dos.write(byteArrayOf('S'.code.toByte(), 'R'.code.toByte(), 'K'.code.toByte(), 'S'.code.toByte()))
        dos.writeByte(1) // version
    }

    fun write(frame: RawFrame) {
        dos.writeLong(frame.tsMs)
        dos.writeInt(frame.width)
        dos.writeInt(frame.height)
        dos.writeInt(frame.rotationDeg)
        dos.writeInt(frame.nv21.size)
        dos.write(frame.nv21)
        count++
    }

    fun seal(): File {
        dos.flush()
        dos.close()

        // COUNT를 헤더에 넣지 않고 끝에 푸터로 기록(간단화)
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(file.length())
            raf.writeLong(count)
        }
        return file
    }

    override fun close() {
        try { dos.close() } catch (_: Exception) {}
    }

    companion object Reader {
        data class ReaderState(val dis: DataInputStream, val totalCount: Long)

        fun open(file: File): ReaderState {
            val fis = BufferedInputStream(FileInputStream(file))
            val dis = DataInputStream(fis)
            val magic = ByteArray(4)
            dis.readFully(magic)
            require(String(magic) == "SRKS") { "Bad magic" }
            val ver = dis.readByte().toInt()
            require(ver == 1) { "Bad version: $ver" }

            // totalCount는 파일 끝에서 읽음
            val raf = RandomAccessFile(file, "r")
            raf.seek(file.length() - 8)
            val total = raf.readLong()
            raf.close()

            return ReaderState(dis, total)
        }

        fun readNext(state: ReaderState): RawFrame? {
            return try {
                val ts = state.dis.readLong()
                val w = state.dis.readInt()
                val h = state.dis.readInt()
                val rot = state.dis.readInt()
                val len = state.dis.readInt()
                val buf = ByteArray(len)
                state.dis.readFully(buf)
                RawFrame(ts, w, h, rot, buf)
            } catch (e: EOFException) {
                null
            }
        }

        fun close(state: ReaderState) {
            try { state.dis.close() } catch (_: Exception) {}
        }
    }
}

// ---------- NV21 / Bitmap 유틸 ----------

/** ImageProxy(YUV_420_888) → NV21 빠른 복사 */
fun imageToNV21(image: ImageProxy): ByteArray {
    require(image.format == ImageFormat.YUV_420_888)
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val ySize = yPlane.buffer.remaining()
    val uSize = uPlane.buffer.remaining()
    val vSize = vPlane.buffer.remaining()

    // NV21 크기 계산
    val nv21 = ByteArray(ySize + uSize + vSize)

    // Y 그대로
    yPlane.buffer.get(nv21, 0, ySize)

    // U/V를 NV21(VU) 순서로 재배열
    // 많은 기기에서 U/V가 interleaved가 아님. plane별 stride 고려 없이 안전 접근하려면
    // 표준 방식으로 행 단위 복사를 해야 정확하지만, 실전에서 아래 간소화로 충분한 경우가 많음.
    // 더 엄밀히 하려면 rowStride/PixelStride를 고려한 루프 구현 필요.
    val uvPixelStride = uPlane.pixelStride
    val uvRowStride = uPlane.rowStride
    val w = image.width
    val h = image.height
    val chromaHeight = h / 2
    val chromaWidth = w / 2

    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer
    uBuf.rewind()
    vBuf.rewind()

    var offset = ySize
    val uRow = ByteArray(uvRowStride)
    val vRow = ByteArray(vPlane.rowStride)

    for (row in 0 until chromaHeight) {
        uBuf.get(uRow, 0, min(uvRowStride, uBuf.remaining()))
        vBuf.get(vRow, 0, min(vPlane.rowStride, vBuf.remaining()))
        var col = 0
        while (col < chromaWidth) {
            val u = uRow[col * uvPixelStride].toInt()
            val v = vRow[col * vPlane.pixelStride].toInt()
            // NV21: V,U interleave
            nv21[offset++] = v.toByte()
            nv21[offset++] = u.toByte()
            col++
        }
    }
    return nv21
}

/** NV21 → Bitmap (빠르고 단순). 회전은 후처리에서 필요시 적용 */
fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap {
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val baos = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, width, height), 90, baos)
    val jpegBytes = baos.toByteArray()
    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}

// ---------- MediaPipe VIDEO 처리기 ----------

class MediaPipeVideoProcessor(
    private val context: Context,
    private val onPose: (tsMs: Long, result: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult) -> Unit,
    private val onHand: (tsMs: Long, result: com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult) -> Unit
) : Closeable {

    val pose: PoseLandmarker
    val hand: HandLandmarker

    init {
        val poseBase = BaseOptions.builder()
            .setModelAssetPath("models/pose_landmarker_lite.task")
            .build()

        val handBase = BaseOptions.builder()
            .setModelAssetPath("models/hand_landmarker.task")
            .build()

        pose = PoseLandmarker.createFromOptions(
            context,
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBase)
                .setRunningMode(RunningMode.VIDEO) // ★ 모든 프레임 처리
                .build()
        )

        hand = HandLandmarker.createFromOptions(
            context,
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBase)
                .setNumHands(2)
                .setRunningMode(RunningMode.VIDEO) // ★ 모든 프레임 처리
                .build()
        )
    }

    fun process(frame: RawFrame) {
        // 회전이 필요하면 Bitmap 회전 추가(여기선 생략; 필요 시 Matrix 사용)
        val bmp = nv21ToBitmap(frame.nv21, frame.width, frame.height)
        val mpImage = BitmapImageBuilder(bmp).build()

        val poseResult = pose.detectForVideo(mpImage, frame.tsMs)
        onPose(frame.tsMs, poseResult)

        val handResult = hand.detectForVideo(mpImage, frame.tsMs)
        onHand(frame.tsMs, handResult)
    }

    override fun close() {
        try { pose.close() } catch (_: Exception) {}
        try { hand.close() } catch (_: Exception) {}
    }
}

// ---------- 메인 컨트롤러: 바인딩/수집/워커 ----------

class FullFrameCaptureController(
    private val context: Context,
    private val onPose: (Long, com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult) -> Unit,
    private val onHand: (Long, com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult) -> Unit,
    private val isChartCreationMode: Boolean = false
) : ImageAnalysis.Analyzer, Closeable {

    private var cameraExecutor: ExecutorService? = null
    private var analysis: ImageAnalysis? = null

    private val frameQueue = ArrayBlockingQueue<RawFrame>(QUEUE_CAPACITY)
    private var spooler: FrameSpooler? = null
    private var workerJob: Job? = null
    @Volatile private var recording = false

    private lateinit var processor: MediaPipeVideoProcessor
    
    // 채보만들기 모드용 프레임 저장소
    private val collectedFrames = mutableListOf<RawFrame>()
    private val frameMutex = ReentrantLock()

    @OptIn(ExperimentalCamera2Interop::class)
    suspend fun start(lifecycleOwner: androidx.lifecycle.LifecycleOwner, fps: Int = 30) {
        processor = MediaPipeVideoProcessor(context, onPose, onHand)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build()

        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER) // ★ 드랍 금지
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        
        // Camera2Interop로 FPS 고정 시도(기기 지원 한계 있음)
        try {
            val ext = Camera2Interop.Extender(analysisBuilder)
            ext.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                android.util.Range(fps, fps)
            )
        } catch (e: Exception) {
            Log.w(TAG, "FPS range set failed: ${e.message}")
        }
        
        analysis = analysisBuilder.build()
        analysis!!.setAnalyzer(cameraExecutor!!, this)

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)

        recording = true
        workerJob = CoroutineScope(Dispatchers.Default).launch {
            runWorkerLoop()
        }
    }

    fun stop() {
        recording = false
        workerJob?.cancel()
        workerJob = null
        analysis?.clearAnalyzer()
        analysis = null
        cameraExecutor?.shutdown()
        try { cameraExecutor?.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS) } catch (_: Exception) {}
        cameraExecutor = null

        // 스풀 종료
        spooler?.let {
            val f = it.seal()
            Log.i(TAG, "Spool sealed: ${f.absolutePath}")
            it.close()
        }
        spooler = null

        try { processor.close() } catch (_: Exception) {}
    }

    override fun close() { stop() }

    // ---------- Analyzer: '복사만' 하고 즉시 종료 ----------
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        try {
            val img = image.image ?: return
            val tsMs = TimeUnit.NANOSECONDS.toMillis(image.imageInfo.timestamp)

            val nv21 = imageToNV21(image)
            val rf = RawFrame(
                tsMs = tsMs,
                width = image.width,
                height = image.height,
                rotationDeg = image.imageInfo.rotationDegrees,
                nv21 = nv21
            )

            if (isChartCreationMode) {
                // 채보만들기 모드: 프레임 수집만 수행 (MediaPipe 처리 안함)
                frameMutex.lock()
                try {
                    collectedFrames.add(rf)
                } finally {
                    frameMutex.unlock()
                }
                Log.d(TAG, "프레임 수집: ts=${tsMs}ms, 총수집=${collectedFrames.size}")
            } else {
                // 실시간 모드: 기존 로직
                val offered = frameQueue.offer(rf)
                if (!offered) {
                    // 큐가 가득 찼다면 파일 스풀로 우회 (최초 1회 생성)
                    if (spooler == null) {
                        spooler = FrameSpooler(context)
                        Log.w(TAG, "Queue full → start spooling to file")
                    }
                    try {
                        spooler?.write(rf)
                    } catch (e: Exception) {
                        Log.e(TAG, "Spool write failed", e)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "analyze error", t)
        } finally {
            image.close() // ★ 반드시
        }
    }

    // ---------- Worker: 큐 → MediaPipe VIDEO ----------
    private suspend fun runWorkerLoop() {
        // 1) 큐 비우기
        while (recording) {
            val frame = withContext(Dispatchers.IO) { frameQueue.poll() }
            if (frame != null) {
                safeProcess(frame)
                continue
            }
            // 2) 큐가 비었고 스풀러가 있다면 → 스풀 파일을 순회 처리
            if (spooler != null) {
                val sealed = spooler!!.seal()
                spooler!!.close()
                spooler = null
                processSpoolFile(sealed)
                sealed.delete()
                continue
            }
            delay(1) // idle
        }

        // stop() 호출 시 잔여도 처리
        drainAll()
    }

    private fun safeProcess(frame: RawFrame) {
        try {
            processor.process(frame)
        } catch (e: Exception) {
            Log.e(TAG, "process frame failed: ts=${frame.tsMs}", e)
        }
    }

    private fun processSpoolFile(file: File) {
        try {
            val state = FrameSpooler.open(file)
            var cnt = 0L
            while (true) {
                val f = FrameSpooler.readNext(state) ?: break
                safeProcess(f)
                cnt++
            }
            FrameSpooler.close(state)
            Log.i(TAG, "Spool processed $cnt frames")
        } catch (e: Exception) {
            Log.e(TAG, "processSpoolFile failed", e)
        }
    }

    private fun drainAll() {
        var drained = 0
        while (true) {
            val f = frameQueue.poll() ?: break
            safeProcess(f)
            drained++
        }
        Log.i(TAG, "Drained $drained queued frames at stop()")
    }
    
    // ---------- 채보만들기 모드: 배치 처리 ----------
    
    /**
     * 채보만들기 모드에서 수집된 모든 프레임을 300ms 구간별로 그룹화하고 MediaPipe 처리
     */
    suspend fun processCollectedFramesForChartCreation(): List<SimilarityRequest> {
        if (!isChartCreationMode) {
            Log.w(TAG, "채보만들기 모드가 아닙니다")
            return emptyList()
        }
        
        val frames: List<RawFrame>
        frameMutex.lock()
        try {
            frames = collectedFrames.toList()
        } finally {
            frameMutex.unlock()
        }
        
        if (frames.isEmpty()) {
            Log.w(TAG, "수집된 프레임이 없습니다")
            return emptyList()
        }
        
        Log.d(TAG, "채보만들기 배치 처리 시작: 총 ${frames.size}개 프레임")
        
        // 1. 첫 번째 프레임을 기준으로 상대적 타임스탬프 계산
        val firstTimestamp = frames.minOf { it.tsMs }
        
        // 2. 300ms 구간별로 그룹화
        val groupedFrames = frames.groupBy { frame ->
            val relativeTimestamp = frame.tsMs - firstTimestamp
            (relativeTimestamp / 300) * 300 // 0, 300, 600, 900... 형태로 버킷팅
        }
        
        Log.d(TAG, "그룹화 완료: ${groupedFrames.size}개 구간")
        
        // 3. 각 구간에 대해 MediaPipe 처리
        val segments = mutableListOf<SimilarityRequest>()
        
        groupedFrames.forEach { (timestampMs, framesInGroup) ->
            val processedFrames = mutableListOf<FrameBlock>()
            
            framesInGroup.forEach { rawFrame ->
                val poses = processFrameForChartCreation(rawFrame)
                val frameDto = FrameBlock(
                    frame = rawFrame.tsMs.toInt(), // 타임스탬프를 프레임 번호로 사용
                    poses = poses
                )
                processedFrames.add(frameDto)
            }
            
            if (processedFrames.isNotEmpty()) {
                val segment = SimilarityRequest(
                    type = "PLAY",
                    timestamp = timestampMs,
                    frames = processedFrames
                )
                segments.add(segment)
            }
        }
        
        Log.d(TAG, "채보만들기 배치 처리 완료: ${segments.size}개 세그먼트")
        return segments
    }
    
    /**
     * 개별 프레임에 대해 MediaPipe 처리 (채보만들기용)
     */
    private suspend fun processFrameForChartCreation(rawFrame: RawFrame): List<PoseBlock> {
        try {
            val bmp = nv21ToBitmap(rawFrame.nv21, rawFrame.width, rawFrame.height)
            val mpImage = BitmapImageBuilder(bmp).build()
            
            val poseResult = processor.pose.detectForVideo(mpImage, rawFrame.tsMs)
            val handResult = processor.hand.detectForVideo(mpImage, rawFrame.tsMs)
            
            val poses = mutableListOf<PoseBlock>()
            
            // BODY 포즈 처리
            if (poseResult.landmarks().isNotEmpty()) {
                val landmarks = poseResult.landmarks()[0]
                val coordinates = landmarks.map { landmark ->
                    Coordinate(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        w = landmark.visibility().orElse(null)
                    )
                }
                poses.add(PoseBlock(
                    part = "BODY",
                    coordinates = coordinates
                ))
            } else {
                // BODY 포즈가 없으면 null 좌표로 채움
                poses.add(PoseBlock(
                    part = "BODY",
                    coordinates = (0..22).map { 
                        Coordinate(x = null, y = null, z = null, w = null)
                    }
                ))
            }
            
            // LEFT_HAND 처리
            if (handResult.landmarks().isNotEmpty()) {
                val landmarks = handResult.landmarks()[0]
                val coordinates = landmarks.map { landmark ->
                    Coordinate(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        w = landmark.visibility().orElse(null)
                    )
                }
                poses.add(PoseBlock(
                    part = "LEFT_HAND",
                    coordinates = coordinates
                ))
            } else {
                poses.add(PoseBlock(
                    part = "LEFT_HAND",
                    coordinates = (0..20).map { 
                        Coordinate(x = null, y = null, z = null, w = null)
                    }
                ))
            }
            
            // RIGHT_HAND 처리 (두 번째 손이 있으면 사용, 없으면 null)
            if (handResult.landmarks().size > 1) {
                val landmarks = handResult.landmarks()[1]
                val coordinates = landmarks.map { landmark ->
                    Coordinate(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z(),
                        w = landmark.visibility().orElse(null)
                    )
                }
                poses.add(PoseBlock(
                    part = "RIGHT_HAND",
                    coordinates = coordinates
                ))
            } else {
                poses.add(PoseBlock(
                    part = "RIGHT_HAND",
                    coordinates = (0..20).map { 
                        Coordinate(x = null, y = null, z = null, w = null)
                    }
                ))
            }
            
            return poses
            
        } catch (e: Exception) {
            Log.e(TAG, "프레임 처리 실패: ts=${rawFrame.tsMs}", e)
            return emptyList()
        }
    }
}
