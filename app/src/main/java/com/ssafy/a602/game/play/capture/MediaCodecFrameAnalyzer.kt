package com.ssafy.a602.game.play.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.ssafy.a602.game.play.dto.CoordinateDto
import com.ssafy.a602.game.play.dto.FrameDto
import com.ssafy.a602.game.play.dto.PoseDto
import com.ssafy.a602.game.play.dto.SegmentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * MediaCodec + Software Decoding을 사용한 비디오 프레임 추출 및 MediaPipe 분석
 * ImageReader 대신 Software Decoding을 사용하여 JNI 오류 방지
 */
class MediaCodecFrameAnalyzer {
  
  companion object {
    private const val TAG = "MediaCodecFrameAnalyzer"
    private const val CODEC_TIMEOUT_US = 10000L // 10ms
    private const val FRAME_INTERVAL_MS = 300L // 300ms
    private const val IMAGE_QUEUE_CAPACITY = 2 // 메모리 안전성을 위해 감소
    private const val IMAGE_POLL_TIMEOUT_MS = 100L
    private const val MAX_RETRY_COUNT = 3
  }

  /**
   * MediaCodec + Software Decoding을 사용한 프레임 추출 및 분석
   * 
   * 이 함수는 비디오 파일에서 모든 프레임을 추출하고, 각 프레임에 대해 MediaPipe를 사용하여
   * 포즈(Pose)와 손(Hand) 랜드마크를 분석합니다. 추출된 좌표 데이터는 300ms 단위로 세그먼트화되어
   * 리듬게임 채보 생성에 사용됩니다.
   * 
   * @param context Android 컨텍스트
   * @param videoUri 분석할 비디오 파일의 URI
   * @param poseLandmarker MediaPipe 포즈 랜드마커 (0-22번, 총 23개 신체 부위 좌표 추출)
   * @param handLandmarker MediaPipe 손 랜드마커 (21개 손가락 좌표 추출)
   * @param onProgress 프레임 분석 진행률 콜백 (프레임 수, 시간)
   * @return 300ms 단위로 세그먼트화된 분석 결과 리스트
   */
  suspend fun extractAndAnalyzeWithMediaCodec(
    context: Context,
    videoUri: Uri,
    poseLandmarker: PoseLandmarker?,
    handLandmarker: HandLandmarker?,
    onProgress: (count: Int, ms: Long) -> Unit
  ): List<SegmentDto> = withContext(Dispatchers.IO) {

    // 분석 결과를 저장할 세그먼트 리스트 (300ms 단위로 그룹화)
    val segments = mutableListOf<SegmentDto>()
    var currentSegment: SegmentDto? = null  // 현재 처리 중인 세그먼트
    var frameIndex = 0  // 전역 프레임 인덱스

    // MediaCodec 리소스 관리
    var extractor: MediaExtractor? = null  // 비디오 파일에서 데이터 추출
    var codec: MediaCodec? = null          // 비디오 디코딩

    try {
      // ===== 1단계: 비디오 파일 분석 및 트랙 선택 =====
      // MediaExtractor를 사용하여 비디오 파일에서 비디오 트랙을 찾고 선택
      extractor = MediaExtractor().apply { setDataSource(context, videoUri, null) }
      val track = selectVideoTrack(extractor)
      if (track == null) {
        val errorMsg = "비디오 파일에 비디오 트랙이 없습니다: $videoUri\n" +
                      "파일 크기: ${File(videoUri.path ?: "").length()} bytes\n" +
                      "총 트랙 수: ${extractor.trackCount}"
        Log.e(TAG, "❌ $errorMsg")
        throw IllegalArgumentException(errorMsg)
      }
      extractor.selectTrack(track)  // 비디오 트랙 선택
      
      // 비디오 트랙의 메타데이터 추출
      val format = extractor.getTrackFormat(track)
      val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME)) { "MIME is null" }
      val srcW = format.getInteger(MediaFormat.KEY_WIDTH)      // 비디오 너비
      val srcH = format.getInteger(MediaFormat.KEY_HEIGHT)     // 비디오 높이
      val rotationDeg = if (format.containsKey(MediaFormat.KEY_ROTATION))
        format.getInteger(MediaFormat.KEY_ROTATION) else 0     // 회전 각도
      val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
        format.getLong(MediaFormat.KEY_DURATION) else Long.MAX_VALUE  // 비디오 길이

      Log.d(TAG, "🎯 Track: mime=$mime, ${srcW}x$srcH, rotation=$rotationDeg°, durationUs=$durationUs")

      // ===== 2단계: Software Decoding 방식 설정 =====
      // ImageReader + Surface 방식 대신 Software Decoding 사용 (JNI 오류 방지)
      if (srcW <= 0 || srcH <= 0) {
        throw IllegalArgumentException("잘못된 해상도: ${srcW}x$srcH")
      }
      
      Log.d(TAG, "🔧 Software Decoding 방식 사용 (ImageReader 없음)")
      Log.d(TAG, "🔧 해상도: ${srcW}x$srcH")

      // ===== 3단계: MediaCodec Software Decoder 구성 =====
      // Surface 없이 Software Decoding을 사용하여 메모리 안전성 확보
      try {
        codec = MediaCodec.createDecoderByType(mime)  // 비디오 코덱 생성
        
        Log.d(TAG, "🔧 MediaCodec 구성: Software Decoding (Surface 없음)")
        
        // Surface 없이 Software Decoding 사용 (null 전달)
        // 이렇게 하면 ImageReader의 JNI 오류를 피할 수 있음
        codec.configure(format, null, null, 0)
        codec.start()
        Log.d(TAG, "✅ MediaCodec 시작 완료 (Software Decoding)")
      } catch (e: Exception) {
        Log.e(TAG, "MediaCodec 구성 실패", e)
        throw e
      }

      // ===== 4단계: Software Decoding 루프 (메인 프레임 처리) =====
      // MediaCodec의 입력/출력 버퍼를 순환하면서 비디오 프레임을 디코딩
      val info = MediaCodec.BufferInfo()  // 출력 버퍼 정보
      var inputEos = false   // 입력 스트림 종료 플래그
      var outputEos = false  // 출력 스트림 종료 플래그
      var lastPtsUs = 0L     // 마지막 프레임 타임스탬프
      var inputBufferCount = 0   // 처리된 입력 버퍼 수
      var outputBufferCount = 0  // 처리된 출력 버퍼 수
      var processedFrames = 0    // MediaPipe로 분석된 프레임 수

      Log.d(TAG, "🔄 Software Decoding 루프 시작")
      
      // 메인 디코딩 루프: 모든 프레임을 순차적으로 처리
      decode@ while (!outputEos) {
        // ===== 입력 버퍼 처리 =====
        // 비디오 파일에서 압축된 데이터를 읽어서 MediaCodec에 공급
        if (!inputEos) {
          val inIdx = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)  // 입력 버퍼 인덱스 요청
          if (inIdx >= 0) {
            inputBufferCount++
            val inBuf: ByteBuffer? = codec.getInputBuffer(inIdx)  // 입력 버퍼 획득
            val sampleSize = extractor.readSampleData(inBuf!!, 0)  // 비디오 파일에서 데이터 읽기
            val sampleTimeUs = extractor.sampleTime  // 프레임 타임스탬프
            
            if (sampleSize < 0) {
              // 더 이상 읽을 데이터가 없음 (End of Stream)
              Log.d(TAG, "📥 입력 스트림 종료 (EOS)")
              codec.queueInputBuffer(
                inIdx, 0, 0, 0L,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM  // EOS 플래그 설정
              )
              inputEos = true
            } else {
              // 압축된 비디오 데이터를 MediaCodec에 공급
              if (inputBufferCount % 30 == 0) { // 30개마다 로그
                Log.d(TAG, "📥 입력 버퍼 처리: size=$sampleSize, time=${sampleTimeUs}us")
              }
              codec.queueInputBuffer(
                inIdx, 0, sampleSize, sampleTimeUs,
                extractor.sampleFlags  // 키프레임 등의 플래그
              )
              extractor.advance()  // 다음 샘플로 이동
            }
          } else if (inIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // 입력 버퍼가 준비되지 않음 - 정상적인 상황 (다음 루프에서 재시도)
          }
        }

        // ===== 출력 버퍼 처리 (Software Decoding) =====
        // MediaCodec에서 디코딩된 프레임 데이터를 가져와서 MediaPipe 분석 수행
        val outIdx = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
        when {
          outIdx >= 0 -> {
            // 디코딩된 프레임 데이터가 준비됨
            outputBufferCount++
            val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0  // 스트림 종료 확인
            
            if (outputBufferCount % 10 == 0) { // 10개마다 로그
              Log.d(TAG, "📤 출력 버퍼 처리: size=${info.size}, time=${info.presentationTimeUs}us, flags=${info.flags}")
            }
            
            // ===== Software Decoding: ByteBuffer에서 직접 데이터 추출 =====
            // ImageReader 대신 ByteBuffer에서 직접 YUV 데이터를 추출하여 Bitmap 생성
            val outputBuffer = codec.getOutputBuffer(outIdx)
            if (outputBuffer != null && info.size > 0) {
              processedFrames++
              if (processedFrames % 5 == 0) { // 5개마다 로그
                Log.d(TAG, "🖼️ Software Decoding 프레임: size=${info.size}, time=${info.presentationTimeUs}us")
              }
              
              try {
                // 프레임 타임스탬프 처리
                val ptsUs = info.presentationTimeUs  // 마이크로초 단위 타임스탬프
                lastPtsUs = max(lastPtsUs, ptsUs)
                val ptsMs = TimeUnit.MICROSECONDS.toMillis(lastPtsUs)  // 밀리초 단위로 변환

                // ===== ByteBuffer를 Bitmap으로 변환 (Software Decoding) =====
                // YUV420 데이터를 RGB Bitmap으로 변환
                val bitmap = softwareDecodeToBitmap(outputBuffer, info, srcW, srcH)
                if (bitmap != null && !bitmap.isRecycled) {
                  // MediaPipe용 이미지 객체 생성
                  val mpImage = BitmapImageBuilder(bitmap).build()

                  // ===== MediaPipe 분석 결과 저장 =====
                  val poses = mutableListOf<PoseDto>()

                  // ===== 1. 포즈 분석 (0-22번, 총 23개 신체 부위만 추출) =====
                  // 얼굴, 몸통, 팔 등의 랜드마크 추출 (다리 제외)
                  poseLandmarker?.let { pl ->
                    try {
                      val r = pl.detectForVideo(mpImage, ptsMs)  // MediaPipe 포즈 분석
                      val allLandmarks = r.landmarks().firstOrNull() ?: emptyList()
                      // 0-22번 랜드마크만 필터링 (다리 랜드마크 제외)
                      val body = allLandmarks.take(23).map { l ->
                        // 각 랜드마크의 좌표와 가시성 추출
                        CoordinateDto(l.x(), l.y(), l.z(), l.visibility().orElse(null))
                      }
                      poses += PoseDto(part = "BODY", coordinates = body)  // 0-22번, 총 23개 신체 부위 좌표
                    } catch (e: Exception) {
                      Log.w(TAG, "Pose detect failed @${ptsMs}ms", e)
                      poses += PoseDto("BODY", emptyList())  // 실패 시 빈 좌표
                    }
                  }

                  // ===== 2. 손 분석 (21개 손가락 랜드마크) =====
                  // 왼손과 오른손의 손가락 랜드마크 추출
                  handLandmarker?.let { hl ->
                    try {
                      val r = hl.detectForVideo(mpImage, ptsMs)  // MediaPipe 손 분석
                      val hands = r.landmarks()  // 손 랜드마크 리스트
                      val handed = r.handedness()  // 손 구분 정보 (Left/Right)
                      var left: List<CoordinateDto>? = null   // 왼손 좌표
                      var right: List<CoordinateDto>? = null  // 오른손 좌표
                      
                      // 각 손의 랜드마크를 Left/Right로 분류
                      for (i in hands.indices) {
                        val coords = hands[i].map { l ->
                          // 각 손가락 랜드마크의 좌표와 가시성 추출
                          CoordinateDto(l.x(), l.y(), l.z(), l.visibility().orElse(null))
                        }
                        val label = handed.getOrNull(i)?.firstOrNull()?.categoryName()  // "Left" 또는 "Right"
                        when {
                          label.equals("Left", true)  -> left  = coords   // 왼손 좌표 저장
                          label.equals("Right", true) -> right = coords  // 오른손 좌표 저장
                        }
                      }
                      poses += PoseDto("LEFT_HAND",  left  ?: emptyList())   // 왼손 21개 랜드마크
                      poses += PoseDto("RIGHT_HAND", right ?: emptyList())  // 오른손 21개 랜드마크
                    } catch (e: Exception) {
                      Log.w(TAG, "Hand detect failed @${ptsMs}ms", e)
                      poses += PoseDto("LEFT_HAND", emptyList())   // 실패 시 빈 좌표
                      poses += PoseDto("RIGHT_HAND", emptyList())
                    }
                  }

                  // ===== 3. 300ms 단위 세그먼트화 =====
                  // 프레임들을 300ms 단위로 그룹화하여 세그먼트 생성
                  val bucketTs = (ptsMs / FRAME_INTERVAL_MS) * FRAME_INTERVAL_MS  // 300ms 버킷 계산
                  if (currentSegment == null || currentSegment!!.timestamp != bucketTs) {
                    // 새로운 세그먼트 시작
                    currentSegment?.let { segments += it }  // 이전 세그먼트 저장
                    currentSegment = SegmentDto(
                      type = "PLAY",      // 세그먼트 타입
                      timestamp = bucketTs,  // 세그먼트 시작 시간 (300ms 단위)
                      frames = mutableListOf()  // 이 세그먼트의 프레임들
                    )
                  }

                  // 현재 프레임을 세그먼트에 추가
                  (currentSegment!!.frames as MutableList).add(
                    FrameDto(
                      frame = frameIndex,  // 전역 프레임 인덱스
                      poses = poses        // MediaPipe 분석 결과 (BODY + LEFT_HAND + RIGHT_HAND)
                    )
                  )

                  frameIndex++  // 프레임 인덱스 증가
                  onProgress(frameIndex, ptsMs)  // 진행률 콜백 호출
                } else {
                  Log.w(TAG, "Software Decoding: Bitmap 생성 실패")
                }
              } catch (e: Exception) {
                Log.e(TAG, "Software Decoding 프레임 처리 중 오류", e)
              }
            } else {
              Log.w(TAG, "Software Decoding: 출력 버퍼가 null이거나 크기가 0")
            }
            
            // 출력 버퍼 해제 (렌더링하지 않음)
            codec.releaseOutputBuffer(outIdx, false)
            
            // 스트림 종료 확인
            if (eos) {
              Log.d(TAG, "📤 출력 스트림 종료 (EOS)")
              outputEos = true
            }
          }
          
          outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            val newFormat = codec.outputFormat
            Log.d(TAG, "📤 출력 포맷 변경: $newFormat")
          }
          
          outIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
            Log.d(TAG, "📤 출력 버퍼 변경")
          }
          
          outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
            // 출력 버퍼가 준비되지 않음 - 정상적인 상황
          }
          
          else -> {
            Log.w(TAG, "예상치 못한 출력 인덱스: $outIdx")
          }
        }
    }

    Log.d(TAG, "📊 Software Decoding 통계: inputBuffers=$inputBufferCount, outputBuffers=$outputBufferCount, processedFrames=$processedFrames")

    // 마지막 세그먼트 추가
    currentSegment?.let { segments += it }

    Log.d(TAG, "✅ Software Decoding 분석 종료: segments=${segments.size}, frames=${segments.sumOf { it.frames.size }}")
    segments
  } catch (e: Exception) {
    Log.e(TAG, "Software Decoding 프레임 추출 실패", e)
    throw e
  } finally {
    // 리소스 정리 (ImageReader 관련 제거)
    try {
      codec?.stop()
    } catch (e: Exception) {
      Log.w(TAG, "MediaCodec stop 실패", e)
    }

    try {
      codec?.release()
    } catch (e: Exception) {
      Log.w(TAG, "MediaCodec release 실패", e)
    }

    try {
      extractor?.release()
    } catch (e: Exception) {
      Log.w(TAG, "MediaExtractor release 실패", e)
    }
  }
}

  /**
   * Software Decoding: ByteBuffer를 Bitmap으로 변환
   * 
   * MediaCodec에서 출력된 YUV420 포맷의 ByteBuffer 데이터를 RGB Bitmap으로 변환합니다.
   * 이 함수는 ImageReader 대신 Software Decoding을 사용할 때 필요한 핵심 변환 로직입니다.
   * 
   * @param outputBuffer MediaCodec에서 출력된 ByteBuffer (YUV420 데이터)
   * @param info 출력 버퍼 정보 (크기, 오프셋 등)
   * @param width 비디오 너비
   * @param height 비디오 높이
   * @return 변환된 RGB Bitmap (실패 시 null)
   */
  private fun softwareDecodeToBitmap(
    outputBuffer: ByteBuffer,
    info: MediaCodec.BufferInfo,
    width: Int,
    height: Int
  ): Bitmap? {
    try {
      // ===== 1. ByteBuffer에서 YUV420 데이터 추출 =====
      // MediaCodec에서 출력된 압축 해제된 YUV420 데이터를 바이트 배열로 복사
      val data = ByteArray(info.size)
      outputBuffer.position(info.offset)  // 버퍼 위치를 데이터 시작점으로 설정
      outputBuffer.get(data, 0, info.size)  // YUV420 데이터를 바이트 배열로 복사
      
      // ===== 2. RGB Bitmap 생성 준비 =====
      // ARGB_8888 포맷의 Bitmap 생성 (각 픽셀당 4바이트)
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(width * height)  // RGB 픽셀 데이터 배열
      
      // ===== 3. YUV420 to RGB 변환 =====
      // YUV420 포맷: Y 평면(전체 크기) + U 평면(1/4 크기) + V 평면(1/4 크기)
      var yIndex = 0  // Y 데이터 인덱스
      var uvIndex = width * height  // U/V 데이터 시작 인덱스
      
      // 각 픽셀에 대해 YUV to RGB 변환 수행
      for (y in 0 until height) {
        for (x in 0 until width) {
          // Y, U, V 값 추출 (0-255 범위)
          val yValue = data[yIndex].toInt() and 0xFF  // 밝도 (Luminance)
          val uValue = data[uvIndex + (y / 2) * (width / 2) + (x / 2)].toInt() and 0xFF  // 색차 U
          val vValue = data[uvIndex + (width * height / 4) + (y / 2) * (width / 2) + (x / 2)].toInt() and 0xFF  // 색차 V
          
          // ===== YUV to RGB 변환 공식 =====
          // 표준 YUV to RGB 변환 매트릭스 적용
          val r = (yValue + 1.402 * (vValue - 128)).toInt().coerceIn(0, 255)  // Red 채널
          val g = (yValue - 0.344136 * (uValue - 128) - 0.714136 * (vValue - 128)).toInt().coerceIn(0, 255)  // Green 채널
          val b = (yValue + 1.772 * (uValue - 128)).toInt().coerceIn(0, 255)  // Blue 채널
          
          // ===== ARGB 픽셀 값 생성 =====
          // Alpha(255) + Red + Green + Blue를 32비트 정수로 결합
          pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
          yIndex++  // 다음 Y 값으로 이동
        }
      }
      
      // ===== 4. Bitmap에 픽셀 데이터 설정 =====
      // 변환된 RGB 픽셀 데이터를 Bitmap에 적용
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
      return bitmap
    } catch (e: Exception) {
      Log.e(TAG, "Software Decode to Bitmap 실패", e)
      return null
    }
  }

  /**
   * 비디오 트랙 선택
   */
  private fun selectVideoTrack(extractor: MediaExtractor): Int? {
    Log.d(TAG, "🔍 비디오 트랙 검색 시작: 총 ${extractor.trackCount}개 트랙")
    
    for (i in 0 until extractor.trackCount) {
      val format = extractor.getTrackFormat(i)
      val mime = format.getString(MediaFormat.KEY_MIME)
      
      Log.d(TAG, "📋 트랙 $i: mime=$mime, ${format.getInteger(MediaFormat.KEY_WIDTH)}x${format.getInteger(MediaFormat.KEY_HEIGHT)}, format=$format")
      
      if (mime?.startsWith("video/") == true) {
        Log.d(TAG, "🎯 비디오 트랙 발견: index=$i, mime=$mime, ${format.getInteger(MediaFormat.KEY_WIDTH)}x${format.getInteger(MediaFormat.KEY_HEIGHT)}")
        return i
      }
    }
    
    Log.e(TAG, "❌ 비디오 트랙을 찾을 수 없습니다. 사용 가능한 트랙이 없거나 비디오 트랙이 없습니다.")
    return null
  }
}