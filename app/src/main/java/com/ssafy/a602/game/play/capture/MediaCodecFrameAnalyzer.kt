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
   */
  suspend fun extractAndAnalyzeWithMediaCodec(
    context: Context,
    videoUri: Uri,
    poseLandmarker: PoseLandmarker?,
    handLandmarker: HandLandmarker?,
    onProgress: (count: Int, ms: Long) -> Unit
  ): List<SegmentDto> = withContext(Dispatchers.IO) {

    val segments = mutableListOf<SegmentDto>()
    var currentSegment: SegmentDto? = null
    var frameIndex = 0

    var extractor: MediaExtractor? = null
    var codec: MediaCodec? = null

    try {
      // 1) Demux: 비디오 트랙 선택
      extractor = MediaExtractor().apply { setDataSource(context, videoUri, null) }
      val track = selectVideoTrack(extractor)
      if (track == null) {
        val errorMsg = "비디오 파일에 비디오 트랙이 없습니다: $videoUri\n" +
                      "파일 크기: ${File(videoUri.path ?: "").length()} bytes\n" +
                      "총 트랙 수: ${extractor.trackCount}"
        Log.e(TAG, "❌ $errorMsg")
        throw IllegalArgumentException(errorMsg)
      }
      extractor.selectTrack(track)
      val format = extractor.getTrackFormat(track)
      val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME)) { "MIME is null" }

      val srcW = format.getInteger(MediaFormat.KEY_WIDTH)
      val srcH = format.getInteger(MediaFormat.KEY_HEIGHT)
      val rotationDeg = if (format.containsKey(MediaFormat.KEY_ROTATION))
        format.getInteger(MediaFormat.KEY_ROTATION) else 0

      val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
        format.getLong(MediaFormat.KEY_DURATION) else Long.MAX_VALUE

      Log.d(TAG, "🎯 Track: mime=$mime, ${srcW}x$srcH, rotation=$rotationDeg°, durationUs=$durationUs")

      // 2) Software Decoding 방식 (ImageReader 대신)
      // 해상도 검증
      if (srcW <= 0 || srcH <= 0) {
        throw IllegalArgumentException("잘못된 해상도: ${srcW}x$srcH")
      }
      
      Log.d(TAG, "🔧 Software Decoding 방식 사용 (ImageReader 없음)")
      Log.d(TAG, "🔧 해상도: ${srcW}x$srcH")

      // 3) Software Decoder 구성 (Surface 없이)
      try {
        codec = MediaCodec.createDecoderByType(mime)
        
        Log.d(TAG, "🔧 MediaCodec 구성: Software Decoding (Surface 없음)")
        
        // Surface 없이 Software Decoding 사용
        codec.configure(format, null, null, 0)
        codec.start()
        Log.d(TAG, "✅ MediaCodec 시작 완료 (Software Decoding)")
      } catch (e: Exception) {
        Log.e(TAG, "MediaCodec 구성 실패", e)
        throw e
      }

      // 4) Software Decoding 루프 (ImageReader 없이)
      val info = MediaCodec.BufferInfo()
      var inputEos = false
      var outputEos = false
      var lastPtsUs = 0L
      var inputBufferCount = 0
      var outputBufferCount = 0
      var processedFrames = 0

      Log.d(TAG, "🔄 Software Decoding 루프 시작")
      
      decode@ while (!outputEos) {
        // 입력 공급
        if (!inputEos) {
          val inIdx = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
          if (inIdx >= 0) {
            inputBufferCount++
            val inBuf: ByteBuffer? = codec.getInputBuffer(inIdx)
            val sampleSize = extractor.readSampleData(inBuf!!, 0)
            val sampleTimeUs = extractor.sampleTime
            
            if (sampleSize < 0) {
              Log.d(TAG, "📥 입력 스트림 종료 (EOS)")
              codec.queueInputBuffer(
                inIdx, 0, 0, 0L,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
              )
              inputEos = true
            } else {
              if (inputBufferCount % 30 == 0) { // 30개마다 로그
                Log.d(TAG, "📥 입력 버퍼 처리: size=$sampleSize, time=${sampleTimeUs}us")
              }
              codec.queueInputBuffer(
                inIdx, 0, sampleSize, sampleTimeUs,
                extractor.sampleFlags
              )
              extractor.advance()
            }
          } else if (inIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // 입력 버퍼가 준비되지 않음 - 정상적인 상황
          }
        }

        // 출력 소비 (Software Decoding)
        val outIdx = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
        when {
          outIdx >= 0 -> {
            outputBufferCount++
            val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
            
            if (outputBufferCount % 10 == 0) { // 10개마다 로그
              Log.d(TAG, "📤 출력 버퍼 처리: size=${info.size}, time=${info.presentationTimeUs}us, flags=${info.flags}")
            }
            
            // Software Decoding: ByteBuffer에서 직접 데이터 추출
            val outputBuffer = codec.getOutputBuffer(outIdx)
            if (outputBuffer != null && info.size > 0) {
              processedFrames++
              if (processedFrames % 5 == 0) { // 5개마다 로그
                Log.d(TAG, "🖼️ Software Decoding 프레임: size=${info.size}, time=${info.presentationTimeUs}us")
              }
              
              try {
                val ptsUs = info.presentationTimeUs
                lastPtsUs = max(lastPtsUs, ptsUs)
                val ptsMs = TimeUnit.MICROSECONDS.toMillis(lastPtsUs)

                // ByteBuffer를 Bitmap으로 변환 (Software Decoding)
                val bitmap = softwareDecodeToBitmap(outputBuffer, info, srcW, srcH)
                if (bitmap != null && !bitmap.isRecycled) {
                  val mpImage = BitmapImageBuilder(bitmap).build()

                  val poses = mutableListOf<PoseDto>()

                  // Pose
                  poseLandmarker?.let { pl ->
                    try {
                      val r = pl.detectForVideo(mpImage, ptsMs)
                      val body = r.landmarks().firstOrNull()?.map { l ->
                        CoordinateDto(l.x(), l.y(), l.z(), l.visibility().orElse(null))
                      } ?: emptyList()
                      poses += PoseDto(part = "BODY", coordinates = body)
                    } catch (e: Exception) {
                      Log.w(TAG, "Pose detect failed @${ptsMs}ms", e)
                      poses += PoseDto("BODY", emptyList())
                    }
                  }

                  // Hands (Left / Right 매핑)
                  handLandmarker?.let { hl ->
                    try {
                      val r = hl.detectForVideo(mpImage, ptsMs)
                      val hands = r.landmarks()
                      val handed = r.handedness()
                      var left: List<CoordinateDto>? = null
                      var right: List<CoordinateDto>? = null
                      for (i in hands.indices) {
                        val coords = hands[i].map { l ->
                          CoordinateDto(l.x(), l.y(), l.z(), l.visibility().orElse(null))
                        }
                        val label = handed.getOrNull(i)?.firstOrNull()?.categoryName()
                        when {
                          label.equals("Left", true)  -> left  = coords
                          label.equals("Right", true) -> right = coords
                        }
                      }
                      poses += PoseDto("LEFT_HAND",  left  ?: emptyList())
                      poses += PoseDto("RIGHT_HAND", right ?: emptyList())
                    } catch (e: Exception) {
                      Log.w(TAG, "Hand detect failed @${ptsMs}ms", e)
                      poses += PoseDto("LEFT_HAND", emptyList())
                      poses += PoseDto("RIGHT_HAND", emptyList())
                    }
                  }

                  // 300ms 버킷
                  val bucketTs = (ptsMs / FRAME_INTERVAL_MS) * FRAME_INTERVAL_MS
                  if (currentSegment == null || currentSegment!!.timestamp != bucketTs) {
                    currentSegment?.let { segments += it }
                    currentSegment = SegmentDto(
                      type = "PLAY",
                      timestamp = bucketTs,
                      frames = mutableListOf()
                    )
                  }

                  (currentSegment!!.frames as MutableList).add(
                    FrameDto(
                      frame = frameIndex,
                      poses = poses
                    )
                  )

                  frameIndex++
                  onProgress(frameIndex, ptsMs)
                } else {
                  Log.w(TAG, "Software Decoding: Bitmap 생성 실패")
                }
              } catch (e: Exception) {
                Log.e(TAG, "Software Decoding 프레임 처리 중 오류", e)
              }
            } else {
              Log.w(TAG, "Software Decoding: 출력 버퍼가 null이거나 크기가 0")
            }
            
            codec.releaseOutputBuffer(outIdx, false)
            
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
   */
  private fun softwareDecodeToBitmap(
    outputBuffer: ByteBuffer,
    info: MediaCodec.BufferInfo,
    width: Int,
    height: Int
  ): Bitmap? {
    try {
      // ByteBuffer에서 데이터 추출
      val data = ByteArray(info.size)
      outputBuffer.position(info.offset)
      outputBuffer.get(data, 0, info.size)
      
      // Raw 데이터를 Bitmap으로 변환
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(width * height)
      
      // YUV420 데이터를 RGB로 변환 (간단한 변환)
      var yIndex = 0
      var uvIndex = width * height
      
      for (y in 0 until height) {
        for (x in 0 until width) {
          val yValue = data[yIndex].toInt() and 0xFF
          val uValue = data[uvIndex + (y / 2) * (width / 2) + (x / 2)].toInt() and 0xFF
          val vValue = data[uvIndex + (width * height / 4) + (y / 2) * (width / 2) + (x / 2)].toInt() and 0xFF
          
          // YUV to RGB 변환
          val r = (yValue + 1.402 * (vValue - 128)).toInt().coerceIn(0, 255)
          val g = (yValue - 0.344136 * (uValue - 128) - 0.714136 * (vValue - 128)).toInt().coerceIn(0, 255)
          val b = (yValue + 1.772 * (uValue - 128)).toInt().coerceIn(0, 255)
          
          pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
          yIndex++
        }
      }
      
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