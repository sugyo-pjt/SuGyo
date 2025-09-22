package com.ssafy.a602.term.ui

import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.ssafy.a602.common.navigation.Screen

/* ------------ Design Tokens (기존 프로젝트 컨벤션) ------------ */
private val BgGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFF7EAF7), // very light pink
        Color(0xFFF3E7FF), // lilac tint
        Color(0xFFF5EEF9)
    )
)
private val CardBackground = Color.White
private val LabelColor = Color(0xFF4A4A55)
private val Subtle = Color(0xFF7C7C88)
private val Primary = Color(0xFF8A2BE2)
private val MandatoryBadgeColor = Color(0xFFFF6B6B)
private val OptionalBadgeColor = Color(0xFF4ECDC4)

/**
 * 약관 상세 화면
 */
@Composable
fun TermDetailScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: TermViewModel = hiltViewModel()
) {
    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    // 화면 진입 시 약관 상세 정보 로드
    LaunchedEffect(id) {
        id?.let { termId ->
            viewModel.loadDetail(termId)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {
            // 상단바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = state.detail?.title ?: "약관 상세",
                    color = LabelColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // 오른쪽 공간을 위한 Spacer
                Spacer(modifier = Modifier.size(48.dp))
            }

            // 본문
            when {
                state.loading -> {
                    TermDetailLoadingContent()
                }
                state.error != null -> {
                    TermDetailErrorContent(
                        message = state.error ?: "알 수 없는 오류가 발생했습니다",
                        onRetry = { id?.let(viewModel::loadDetail) }
                    )
                }
                state.detail != null -> {
                    // 본문
                    TermDetailContent(detail = state.detail!!)

                    Spacer(Modifier.height(16.dp))

                    // ✅ 하단 확인 버튼
                    Button(
                        onClick = {
                            // 회원가입 화면으로 이동하면서 자동 체크 신호 세팅
                            try {
                                val signupEntry =
                                    navController.getBackStackEntry(Screen.Signup.route)
                                signupEntry.savedStateHandle.set("TERMS_MANDATORY_AGREED", true)
                            } catch (_: Exception) {
                                // back stack에 Signup이 없을 수도 있으니 무시
                            }
                            navController.navigate(Screen.Signup.route) {
                                launchSingleTop = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("확인", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 약관 상세 컨텐츠
 */
@Composable
private fun TermDetailContent(
    detail: com.ssafy.a602.term.data.model.TermDetailDto
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 약관 정보 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 제목과 뱃지
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (detail.mandatory) MandatoryBadgeColor else OptionalBadgeColor
                    ) {
                        Text(
                            text = if (detail.mandatory) "필수" else "선택",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = detail.title,
                        color = LabelColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 약관 내용 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "약관 내용",
                    color = LabelColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // 약관 본문 (스크롤 가능한 TextView)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 600.dp),
                    factory = { context ->
                        TextView(context).apply {
                            text = detail.content
                            setTextIsSelectable(true)
                            movementMethod = ScrollingMovementMethod()
                            textSize = 14f
                            setTextColor(android.graphics.Color.parseColor("#4A4A55"))
                            setPadding(0, 0, 0, 0)
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 로딩 컨텐츠
 */
@Composable
private fun TermDetailLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "약관을 불러오는 중...",
                color = Subtle,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 에러 컨텐츠
 */
@Composable
private fun TermDetailErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "오류가 발생했습니다",
                color = LabelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Subtle,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "다시 시도",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
