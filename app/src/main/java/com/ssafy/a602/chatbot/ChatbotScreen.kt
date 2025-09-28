package com.ssafy.a602.chatbot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.ssafy.a602.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.a602.chatbot.ChatMessage
import com.ssafy.a602.chatbot.ChatViewModel
import com.ssafy.a602.chatbot.Sender
import kotlinx.coroutines.launch

object ChatTheme {
    val Bg = Color(0xFFF1FBF4) // 다른 스크린과 동일한 연한 초록 배경
    val MyBubble = Color(0xFF4CAF50) // 초록색 버블
    val OtherBubble = Color.White
    val Time = Color(0xFF9AA0A6)
    val TopBarBg = Color(0xFFF1FBF4)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    onBack: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {

    val messages by vm.messages.collectAsState()
    val typing by vm.isBotTyping.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 새 메시지 수신 시 하단 고정
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier
                    .statusBarsPadding(),
                windowInsets = WindowInsets(0),
                title = { 
                    Text(
                        "수어 챗봇", 
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack, 
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                actions = { /* 설정/새로고침 등 필요시 */ }
            )
        },
        containerColor = ChatTheme.Bg,
        bottomBar = { 
            InputBar(
                onSend = vm::sendUserMessage,
                modifier = Modifier
            ) 
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(messages, key = { _, m -> m.id }) { index, msg ->
                    val prev = messages.getOrNull(index - 1)
                    val showAvatarGap =
                        msg.sender == Sender.BOT && (prev == null || prev.sender != msg.sender)
                    MessageBubble(
                        msg = msg,
                        isMine = msg.sender == Sender.USER,
                        showAvatarGap = showAvatarGap,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item {
                    AnimatedVisibility(visible = typing, enter = fadeIn(), exit = fadeOut()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Spacer(Modifier.width(38.dp))
                            TypingDots()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: ChatMessage,
    isMine: Boolean,
    showAvatarGap: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (isMine)
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)

    val bubbleColor = if (isMine) ChatTheme.MyBubble else ChatTheme.OtherBubble

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        if (!isMine) {
            if (showAvatarGap) AvatarStub() else Spacer(Modifier.width(32.dp))
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.weight(1f))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Surface(
                color = bubbleColor, 
                shape = shape, 
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) Color.White else Color.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = com.ssafy.a602.chatbot.ChatViewModel.formatTime(msg.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = ChatTheme.Time
            )
        }

        if (isMine) Spacer(Modifier.width(6.dp)) else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AvatarStub() {
    val sharkPainter: Painter = painterResource(id = R.drawable.babyshark)
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) { 
        Image(
            painter = sharkPainter,
            contentDescription = "AI 챗봇",
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun TypingDots() {
    val t1 = rememberInfiniteTransition()
    val a1 by t1.animateFloat(initialValue = .2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse))
    val a2 by t1.animateFloat(initialValue = 1f, targetValue = .2f,
        animationSpec = infiniteRepeatable(tween(650, delayMillis = 220), RepeatMode.Reverse))
    val a3 by t1.animateFloat(initialValue = .2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, delayMillis = 440), RepeatMode.Reverse))
    Row(
        Modifier.padding(8.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(a1); Spacer(Modifier.width(6.dp)); Dot(a2); Spacer(Modifier.width(6.dp)); Dot(a3)
    }
}

@Composable private fun Dot(alpha: Float) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
}

@Composable
private fun InputBar(
    onSend: (String) -> Unit,
    hint: String = "메시지를 입력하세요",
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(hint) },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChatTheme.MyBubble,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                val t = text.trim()
                if (t.isNotEmpty()) { onSend(t); text = "" }
            },
            modifier = Modifier
                .background(
                    color = ChatTheme.MyBubble,
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Outlined.Send, 
                contentDescription = "전송",
                tint = Color.White
            )
        }
    }
}
