package com.ssafy.a602.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.chatbot.ChatRepository
import com.ssafy.a602.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class Sender { USER, BOT }
enum class MessageType { TEXT, GRAMMAR_CARD } // 확장 대비

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val type: MessageType = MessageType.TEXT,
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = true,
    // 문법카드 확장 필드 (필요 시 사용)
    val grammarTitle: String? = null,
    val grammarPoint: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private var userId: String = tokenManager.getUserId()?.toString() ?: "anonymous"

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping

    fun setUserId(id: String) { userId = id }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // 1) 내 메시지 추가
        _messages.update { it + ChatMessage(sender = Sender.USER, text = trimmed) }

        // 2) 타이핑 인디케이터 on
        _isBotTyping.value = true

        // 3) API 호출
        viewModelScope.launch {
            val result = repo.ask(userId = userId, text = trimmed)
            _isBotTyping.value = false

            _messages.update { list ->
                val botMsg = result.fold(
                    onSuccess = { ChatMessage(sender = Sender.BOT, text = it) },
                    onFailure = {
                        ChatMessage(
                            sender = Sender.BOT,
                            text = "서버 응답에 실패했어요. 잠시 후 다시 시도해 주세요."
                        )
                    }
                )
                list + botMsg
            }
        }
    }

    companion object {
        fun formatTime(millis: Long): String =
            SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(millis))
    }
}
