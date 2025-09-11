package com.ssafy.a602.game.play.input

import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WordWindowUploader(
    private val buffer: LandmarkBuffer3s,
    private val endpoint: String
) {
    @Volatile private var pendingCenter: Long? = null
    @Volatile private var pendingWordId: String? = null

    private val http = OkHttpClient()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun onWord(centerMs: Long, wordId: String) {
        pendingCenter = centerMs; pendingWordId = wordId
        maybeFlush()
    }
    fun maybeFlush() {
        val c = pendingCenter ?: return
        val id = pendingWordId ?: return
        if (buffer.latest() >= c + 1000) {
            val frames = buffer.sliceAround(c)
            val payload = UploadPayload(id, c, frames)
            upload(payload)
            pendingCenter = null; pendingWordId = null
        }
    }
    private fun upload(p: UploadPayload) {
        val body = json.encodeToString(UploadPayload.serializer(), p)
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(endpoint).post(body).build()
        http.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) { android.util.Log.e("UPLOAD","fail",e) }
            override fun onResponse(call: Call, r: Response) { r.close(); android.util.Log.d("UPLOAD","ok") }
        })
    }
}
