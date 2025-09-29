package com.ssafy.a602.game.play.input

import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.ssafy.a602.auth.TokenManager
import javax.inject.Inject

class WordWindowUploader(
    private val buffer: DynamicLandmarkBuffer,
    private val endpoint: String,
    private val tokenManager: TokenManager? = null
) {
    @Volatile private var pendingActionStart: Long? = null
    @Volatile private var pendingActionEnd: Long? = null
    @Volatile private var pendingSegment: Int? = null
    @Volatile private var pendingCorrectStartedIndex: Int? = null
    @Volatile private var pendingCorrectEndedIndex: Int? = null
    @Volatile private var pendingMusicId: Int? = null

    private val http = OkHttpClient()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    /**
     * 수어 타이밍 시작
     * actionStartedAt과 actionEndedAt 시간에 맞춰 버퍼 설정
     */
    fun onWord(
        actionStartMs: Long,
        actionEndMs: Long,
        segment: Int,
        correctStartedIndex: Int,
        correctEndedIndex: Int,
        musicId: Int
    ) {
        pendingActionStart = actionStartMs
        pendingActionEnd = actionEndMs
        pendingSegment = segment
        pendingCorrectStartedIndex = correctStartedIndex
        pendingCorrectEndedIndex = correctEndedIndex
        pendingMusicId = musicId

        // 동적 버퍼에 수어 타이밍 설정
        val wordId = "${segment}_${correctStartedIndex}_${correctEndedIndex}"
        buffer.setActionTiming(actionStartMs, actionEndMs, wordId)

        android.util.Log.d("WordWindowUploader", "수어 타이밍 시작: ${actionStartMs}ms ~ ${actionEndMs}ms, segment: $segment, range: $correctStartedIndex~$correctEndedIndex, musicId: $musicId")
    }

    /**
     * 수어 타이밍 종료 시 데이터 업로드
     */
    fun onActionEnd() {
        val startTime = pendingActionStart ?: return
        val endTime = pendingActionEnd ?: return
        val segment = pendingSegment ?: return
        val correctStartedIndex = pendingCorrectStartedIndex ?: return
        val correctEndedIndex = pendingCorrectEndedIndex ?: return
        val musicId = pendingMusicId ?: return

        // 수어 타이밍 범위 내의 프레임들 추출
        val frames = buffer.sliceForAction()

        if (frames.isNotEmpty()) {
            val payload = UploadPayload(
                musicId = musicId,
                segment = segment,
                frames = frames.map { it.toUploadFrame() }
            )

            // 업로드할 데이터 상세 정보 출력
            android.util.Log.d("WordWindowUploader", "=== 업로드 데이터 상세 정보 ===")
            android.util.Log.d("WordWindowUploader", "musicId: $musicId")
            android.util.Log.d("WordWindowUploader", "segment: $segment")
            android.util.Log.d("WordWindowUploader", "correctRange: $correctStartedIndex~$correctEndedIndex")
            android.util.Log.d("WordWindowUploader", "시간 범위: ${startTime}ms ~ ${endTime}ms")
            android.util.Log.d("WordWindowUploader", "프레임 수: ${frames.size}")

            // 첫 번째와 마지막 프레임 정보
            val firstFrame = frames.first()
            val lastFrame = frames.last()
            android.util.Log.d("WordWindowUploader", "첫 프레임: ${firstFrame.tsMs}ms")
            android.util.Log.d("WordWindowUploader", "마지막 프레임: ${lastFrame.tsMs}ms")

            // 첫 번째 프레임의 랜드마크 샘플
            android.util.Log.d("WordWindowUploader", "첫 프레임 랜드마크 샘플:")
            android.util.Log.d("WordWindowUploader", "  포즈: ${firstFrame.pose.size}개 (예: ${firstFrame.pose.take(2).joinToString { lm ->
                if (lm != null) "(${String.format("%.2f", lm.x)}, ${String.format("%.2f", lm.y)})" else "null"
            }})")
            android.util.Log.d("WordWindowUploader", "  왼손: ${firstFrame.left.size}개 (예: ${firstFrame.left.take(2).joinToString { lm ->
                if (lm != null) "(${String.format("%.2f", lm.x)}, ${String.format("%.2f", lm.y)})" else "null"
            }})")
            android.util.Log.d("WordWindowUploader", "  오른손: ${firstFrame.right.size}개 (예: ${firstFrame.right.take(2).joinToString { lm ->
                if (lm != null) "(${String.format("%.2f", lm.x)}, ${String.format("%.2f", lm.y)})" else "null"
            }})")

            // 상세 좌표 데이터 로깅 (업로드용)
            buffer.logCoordinatesForUpload(frames)

            upload(payload)
            android.util.Log.d("WordWindowUploader", "수어 데이터 업로드 완료: ${frames.size}개 프레임, segment: $segment")
            android.util.Log.d("WordWindowUploader", "================================")
        } else {
            android.util.Log.w("WordWindowUploader", "업로드할 프레임이 없음: segment: $segment")
        }

        // 수어 타이밍 해제
        buffer.clearActionTiming()
        pendingActionStart = null
        pendingActionEnd = null
        pendingSegment = null
        pendingCorrectStartedIndex = null
        pendingCorrectEndedIndex = null
        pendingMusicId = null
    }


    /**
     * 기존 호환성을 위한 메서드
     */
    fun maybeFlush() {
        // 동적 버퍼에서는 onActionEnd()를 명시적으로 호출해야 함
        if (buffer.isActionActive()) {
            android.util.Log.d("WordWindowUploader", "maybeFlush 호출됨 - onActionEnd() 사용 권장")
        }
    }
    private fun upload(p: UploadPayload) {
        val body = json.encodeToString(UploadPayload.serializer(), p)
            .toRequestBody("application/json".toMediaType())

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body)

        // 인증 토큰이 있으면 Authorization 헤더 추가
        val accessToken = tokenManager?.getAccessToken()
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        val req = requestBuilder.build()
        http.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("UPLOAD","fail",e)
            }
            override fun onResponse(call: Call, r: Response) {
                android.util.Log.d("UPLOAD","ok - status: ${r.code}")
                r.close()
            }
        })
    }
}
