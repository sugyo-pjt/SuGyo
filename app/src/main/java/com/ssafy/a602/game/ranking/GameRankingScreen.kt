package com.ssafy.a602.game.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.ssafy.a602.game.GameTheme

// 색상 팔레트 (GameTheme 사용)
private val BackgroundGradient = GameTheme.Colors.BackgroundGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRankingScreen(
    songId: String,
    onBackClick: () -> Unit = {}
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<GameRankingViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    
    // 순위 데이터 로드
    LaunchedEffect(songId) {
        viewModel.loadRankings(songId)
    }
    Scaffold(
        topBar = {
            // 고정 높이의 커스텀 상단바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp) // 고정 높이 설정
                    .background(Color(0xFF1B2454))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 뒤로가기 버튼
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                    
                    // 제목 (중앙 정렬)
                    Text(
                        text = "명예의 전당",
                        style = GameTheme.Typography.ScreenTitle,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // 오른쪽 공간 (뒤로가기 버튼과 균형 맞추기)
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 곡이름 표시
                Text(
                    text = uiState.songTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(30.dp))

                // 로딩 상태 처리
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error ?: "알 수 없는 오류가 발생했습니다.",
                                color = Color.Red,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        // Top 3 순위 표시
                        Top3PodiumRow(
                            items = uiState.top3Rankings.map { ranking ->
                                PodiumItem(ranking.rank, ranking.nickname, ranking.formattedScore)
                            }
                        )
                        
                        // 내 순위 표시
                        uiState.myRanking?.let { myRanking ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "내 순위",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RankRow(
                                item = RankItem(
                                    rank = myRanking.rank,
                                    name = myRanking.nickname,
                                    score = myRanking.score,
                                    playedDate = myRanking.playedDate,
                                    avatarUrl = myRanking.avatarUrl,
                                    isMe = true
                                )
                            )
                        }
                        
                        // 전체 순위 표시 (10위까지만)
                        if (uiState.allRankings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(30.dp))
                            Text(
                                text = "전체 순위",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // LazyColumn으로 스크롤 가능한 순위 리스트
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp), // 고정 높이로 스크롤 영역 제한
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items(
                                    items = uiState.allRankings.take(10).map { ranking ->
                                        RankItem(
                                            rank = ranking.rank,
                                            name = ranking.nickname,
                                            score = ranking.score,
                                            playedDate = ranking.playedDate,
                                            avatarUrl = ranking.avatarUrl,
                                            isMe = ranking.isMe
                                        )
                                    }
                                ) { item ->
                                    RankRow(
                                        item = item,
                                        showDivider = item.rank < 10 // 마지막 항목에는 구분선 없음
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




// ===== Top3 Podium =====

data class PodiumItem(
    val rank: Int,          // 1,2,3
    val name: String,       // 닉네임
    val score: String       // "987,650" 처럼 포맷된 문자열
)

@Composable
fun Top3PodiumRow(
    items: List<PodiumItem>,
    modifier: Modifier = Modifier
) {
    // 1위가 가운데 오도록 정렬
    val sorted = items.sortedBy { it.rank }
    val first = sorted.firstOrNull { it.rank == 1 }
    val second = sorted.firstOrNull { it.rank == 2 }
    val third = sorted.firstOrNull { it.rank == 3 }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2위 (좌)
        second?.let {
            PodiumCard(
                item = it,
                width = 64.dp,
                height = 100.dp,
                avatarSize = 43.dp
            )
        }
        // 1위 (가운데, 더 크고 약간 위로)
        first?.let {
            PodiumCard(
                item = it,
                width = 80.dp,
                height = 120.dp,
                avatarSize = 51.dp,
            )
        }
        // 3위 (우)
        third?.let {
            PodiumCard(
                item = it,
                width = 64.dp,
                height = 100.dp,
                avatarSize = 43.dp
            )
        }
    }
}

@Composable
private fun PodiumCard(
    item: PodiumItem,
    width: Dp,
    height: Dp,
    avatarSize: Dp
) {
    val gradient = when (item.rank) {
        1 -> Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFF9A825))) // 골드
        2 -> Brush.verticalGradient(listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE))) // 실버
        else -> Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00))) // 브론즈
    }

    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 카드(위) + 아바타 겹치기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.TopCenter
        ) {
            // 카드 본체 (배지만 제거, 하단에 등수 텍스트)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height - avatarSize / 2),
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .background(gradient)
                        .padding(top = avatarSize / 2 + 8.dp, bottom = 18.dp)
                        .fillMaxSize()
                ) {
                    // 등수: 흰 글씨, 하단 중앙
                    Text(
                        text = item.rank.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 25.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            // 아바타(겹침)
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .offset(y = (-avatarSize / 2))
                    .background(Color.White, shape = CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                // TODO: 실제 이미지가 있으면 Avatar(url, avatarSize)로 교체
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF374151))
                )
            }
        }

        // 카드 아래 닉네임 / 점수
        Spacer(Modifier.height(1.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = item.score,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFFE082)
        )
    }
}

data class RankItem(
    val rank: Int,
    val name: String,
    val score: Int,
    val playedDate: LocalDate,   // 2024-03-15 처럼
    val avatarUrl: String? = null,
    val isMe: Boolean = false
)

// -------- 단일 행 컴포넌트 --------
@Composable
fun RankRow(
    item: RankItem,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    val scoreText = NumberFormat.getNumberInstance(Locale.KOREA).format(item.score)
    val dateText = item.playedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    val medalEmoji = when (item.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }

    // 내 항목 강조 배경 (목업처럼 살짝 밝은 카드)
    val containerModifier = if (item.isMe) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x3346A0FF), Color(0x331C75FF)) // 은은한 하이라이트
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    }

    Column {
        Row(
            modifier = containerModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위/메달
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (medalEmoji != null) {
                    Text(medalEmoji, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = item.rank.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 아바타
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D3A6A)),
                contentAlignment = Alignment.Center
            ) {
                if (item.avatarUrl != null) {
                    AsyncImage(
                        model = item.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 기본 플레이스홀더
                    Text("👤", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            // 닉네임 + 날짜
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateText,
                    color = Color(0xFFB9C2E5),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // 점수 (우측 정렬, 굵게)
            Text(
                text = scoreText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (showDivider && !item.isMe) {
            Divider(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )
        }
    }
}




@Preview(showBackground = true)
@Composable
fun GameRankingScreenPreview() {
    GameRankingScreen(
        songId = "way_back_home",
        onBackClick = {}
    )
}
