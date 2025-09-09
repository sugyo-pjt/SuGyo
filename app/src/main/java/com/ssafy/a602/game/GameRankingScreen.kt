package com.ssafy.a602.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// 색상 팔레트
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF3D2C8D), // 보라
        Color(0xFF1F2A6B), // 남색
        Color(0xFF0E2149)  // 진한 남색
    )
)

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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "명예의 전당",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1B2454)
                )
            )
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


@Composable
private fun RankBadge(rank: Int) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.25f)
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Avatar(url: String, size: Dp) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}



@Preview(showBackground = true)
@Composable
fun GameRankingScreenPreview() {
    GameRankingScreen(
        songId = "way_back_home",
        onBackClick = {}
    )
}
