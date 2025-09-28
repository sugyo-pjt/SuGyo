package com.ssafy.a602.chatbot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("sentence") val sentence: String
)

@Serializable
data class ChatResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("result") val result: String
)
