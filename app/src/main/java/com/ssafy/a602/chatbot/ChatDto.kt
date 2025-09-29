package com.ssafy.a602.chatbot

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("sentence") val sentence: String
)

data class ChatResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("result") val result: String
)
