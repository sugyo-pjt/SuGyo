package com.ssafy.a602.chatbot

import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {
    @POST("/fastapi/chat")
    suspend fun chat(@Body body: ChatRequest): ChatResponse
}
