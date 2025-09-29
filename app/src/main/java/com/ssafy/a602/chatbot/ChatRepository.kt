package com.ssafy.a602.chatbot

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: ChatApi
) {
    suspend fun ask(userId: String, text: String): Result<String> = runCatching {
        api.chat(ChatRequest(userId = userId, sentence = text)).result
    }
}
