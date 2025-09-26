package com.ssafy.a602.game.play.capture

import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.media.ImageReader
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

object MediaCodecFrameAnalyzer {

  // 분석 결과(프레임 단위)
  data class FrameResult(
    val index: Int,
    val ptsMs: Long,
    val body: List<LandmarkerManager.LM>,
    val left: List<LandmarkerManager.LM>,
    val right: List<LandmarkerManager.LM>
  )

  /**
   * MP4 전체 프레임 순차 추출 + MediaPipe(VIDEO) 분석
   * @param timeLimitUs 분석 상한(기본 60초). 더 길면 늘리세요.
   * @param downscaleToWidth 저장/분석용 다운스케일 폭(성능/발열↓)
   * @param onProgress 프레임 수/타임스탬프 콜백
   * @param onFrame 프레임별 결과 콜백(서버 DTO 누적 등)
   * @return 처리된 프레임 총 수
   */
  suspend fun analyzeVideo(
    context: Context,
    videoUri: Uri,
    landmarker: LandmarkerManager,
    timeLimitUs: Long = 60_000_000L,
    downscaleToWidth: Int = 640,
    onProgress: (count: Int, ptsMs: Long) -> Unit = { _, _ -> },
    onFrame: (FrameResult) -> Unit
  ): Int = withContext(Dispatchers.IO) {

    // 1) Demux
    val extractor = MediaExtractor().apply { setDataSource(context, videoUri, null) }
    val track = (0 until extractor.trackCount).firstOrNull { i ->
      extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
    } ?: throw IllegalArgumentException("No video track")
    extractor.selectTrack(track)
    val format = extractor.getTrackFormat(track)
    val mime = format.getString(MediaFormat.KEY_MIME)!!
    val srcW = format.getInteger(MediaFormat.KEY_WIDTH)
    val srcH = format.getInteger(MediaFormat.KEY_HEIGHT)
    val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else Long.MAX_VALUE
    val limitUs = minOf(timeLimitUs, durationUs)
    val rotationDeg = if (format.containsKey(MediaFormat.KEY_ROTATION)) format.getInteger(MediaFormat.KEY_ROTATION) else 0

    // 2) 출력 Surface 준비(ImageReader)
    val imageReader = ImageReader.newInstance(srcW, srcH, ImageFormat.YUV_420_888, 6)
    val surface: Surface = imageReader.surface
    val imageQueue = ArrayBlockingQueue<Image>(6)
    val ht = HandlerThread("DecodeImageThread").apply { start() }
    imageReader.setOnImageAvailableListener({ r ->
      r.acquireNextImage()?.let { img ->
        if (!imageQueue.offer(img)) { imageQueue.poll()?.close(); imageQueue.offer(img) }
      }
    }, Handler(ht.looper))

    // 3) Decoder 구성
    val codec = MediaCodec.createDecoderByType(mime).apply {
      configure(format, surface, null, 0)
      start()
    }

    // 4) 디코딩 루프
    var frames = 0
    var inputEos = false
    var lastPtsUs = 0L

    decode@ while (true) {
      // 입력 공급
      if (!inputEos) {
        val inIdx = codec.dequeueInputBuffer(10_000)
        if (inIdx >= 0) {
          val buf: ByteBuffer? = if (Build.VERSION.SDK_INT >= 21) codec.getInputBuffer(inIdx)
                                  else codec.inputBuffers[inIdx]
          val size = extractor.readSampleData(buf!!, 0)
          val st = extractor.sampleTime
          if (size < 0 || st > limitUs) {
            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            inputEos = true
          } else {
            codec.queueInputBuffer(inIdx, 0, size, st, extractor.sampleFlags)
            extractor.advance()
          }
        }
      }

      // 출력 소비
      val info = MediaCodec.BufferInfo()
      val outIdx = codec.dequeueOutputBuffer(info, 10_000)

      when {
        outIdx >= 0 -> {
          val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
          codec.releaseOutputBuffer(outIdx, /* render */ true)

          // 렌더된 프레임 수신
          val image = imageQueue.poll(50, TimeUnit.MILLISECONDS)
          if (image != null) {
            val ptsUs = TimeUnit.NANOSECONDS.toMicros(image.timestamp) // ns → us
            lastPtsUs = max(lastPtsUs, ptsUs)
            var bmp = yuvToBitmap(image) // ARGB
            image.close()

            // 회전/미러링 보정 (전면 카메라 미러링 포함)
            bmp = rotateAndMirrorIfNeeded(bmp, rotationDeg, mirror = true)
            // 다운스케일
            if (downscaleToWidth in 1 until bmp.width) {
              val h = (bmp.height * (downscaleToWidth.toFloat() / bmp.width)).toInt().coerceAtLeast(1)
              bmp = Bitmap.createScaledBitmap(bmp, downscaleToWidth, h, true)
            }

            // MediaPipe VIDEO (ms 단위!)
            val ptsMs = TimeUnit.MICROSECONDS.toMillis(lastPtsUs)
            val mpImage = BitmapImageBuilder(bmp).build()
            val poseR = landmarker.pose.detectForVideo(mpImage, ptsMs)
            val handR = landmarker.hand.detectForVideo(mpImage, ptsMs)
            bmp.recycle()

            val body = landmarker.mapPose(poseR)
            val (left, right) = landmarker.mapHands(handR)

            onFrame(FrameResult(frames, ptsMs, body, left, right))
            frames++
            if (frames % 30 == 0) onProgress(frames, ptsMs)
          }

          if (eos) {
            // EOS 후 잔여 프레임 처리 (Drain 보강)
            var spins = 0
            while (!imageQueue.isEmpty() && spins < 3) {
              val remainingImage = imageQueue.poll(30, TimeUnit.MILLISECONDS)
              if (remainingImage != null) {
                val remainingPtsUs = TimeUnit.NANOSECONDS.toMicros(remainingImage.timestamp)
                lastPtsUs = max(lastPtsUs, remainingPtsUs)
                var remainingBmp = yuvToBitmap(remainingImage)
                remainingImage.close()

                // 회전/미러링 보정
                remainingBmp = rotateAndMirrorIfNeeded(remainingBmp, rotationDeg, mirror = true)
                // 다운스케일
                if (downscaleToWidth in 1 until remainingBmp.width) {
                  val h = (remainingBmp.height * (downscaleToWidth.toFloat() / remainingBmp.width)).toInt().coerceAtLeast(1)
                  remainingBmp = Bitmap.createScaledBitmap(remainingBmp, downscaleToWidth, h, true)
                }

                // MediaPipe VIDEO 처리
                val remainingPtsMs = TimeUnit.MICROSECONDS.toMillis(lastPtsUs)
                val remainingMpImage = BitmapImageBuilder(remainingBmp).build()
                val remainingPoseR = landmarker.pose.detectForVideo(remainingMpImage, remainingPtsMs)
                val remainingHandR = landmarker.hand.detectForVideo(remainingMpImage, remainingPtsMs)
                remainingBmp.recycle()

                val remainingBody = landmarker.mapPose(remainingPoseR)
                val (remainingLeft, remainingRight) = landmarker.mapHands(remainingHandR)

                onFrame(FrameResult(frames, remainingPtsMs, remainingBody, remainingLeft, remainingRight))
                frames++
              }
              spins++
            }
            break@decode
          }
        }

        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* 필요 시 로그 */ }
        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
          if (inputEos && imageQueue.isEmpty()) break@decode
        }
      }
    }

    // 5) 해제
    try { codec.stop() } catch(_:Exception){}
    codec.release()
    imageReader.close()
    ht.quitSafely()
    extractor.release()

    frames
  }

  /** 회전/미러링 보정 유틸 */
  private fun rotateAndMirrorIfNeeded(src: Bitmap, rotationDeg: Int, mirror: Boolean = false): Bitmap {
    if (rotationDeg == 0 && !mirror) return src
    val m = Matrix()
    if (mirror) m.postScale(-1f, 1f, src.width / 2f, src.height / 2f)
    if (rotationDeg != 0) m.postRotate(rotationDeg.toFloat())
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true).also {
      if (it !== src) src.recycle()
    }
  }

  /** YUV_420_888 → NV21 → JPEG → Bitmap (간단·호환성 높음) */
  private fun yuvToBitmap(image: Image): Bitmap {
    require(image.format == ImageFormat.YUV_420_888)
    val nv21 = yuv420888ToNv21(image)
    val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val bos = java.io.ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 100, bos)
    val bytes = bos.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  }

  private fun yuv420888ToNv21(image: Image): ByteArray {
    val y = image.planes[0]; val u = image.planes[1]; val v = image.planes[2]
    val w = image.width; val h = image.height
    val out = ByteArray(w * h + (w * h / 2))

    // Y
    var p = 0
    for (row in 0 until h) {
      var off = row * y.rowStride
      var col = 0
      while (col < w) {
        out[p++] = y.buffer.get(off)
        off += y.pixelStride
        col++
      }
    }
    // VU (NV21)
    val chromaH = h / 2; val chromaW = w / 2
    var uvPos = w * h
    for (row in 0 until chromaH) {
      var uOff = row * u.rowStride
      var vOff = row * v.rowStride
      for (col in 0 until chromaW) {
        val U = u.buffer.get(uOff)
        val V = v.buffer.get(vOff)
        out[uvPos++] = V
        out[uvPos++] = U
        uOff += u.pixelStride
        vOff += v.pixelStride
      }
    }
    return out
  }
}
