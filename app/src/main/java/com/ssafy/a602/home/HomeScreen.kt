package com.ssafy.a602.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.TextUnit // [추가] 폰트 사이즈 타입을 쓰기 위해 임포트

@Composable
fun HomeScreen(
    onGoLearning: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenGame: () -> Unit = {},
    onOpenMyPage: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 브랜드/배지 (상단 알림 카드 느낌)
        BrandBanner()

        Spacer(Modifier.height(24.dp))

        // 인사 영역
        Text(
            text = "안녕하세요! 👋",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "오늘도 수어지교와 함께 성장해요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // 기능 카드 2x2 그리드
        FeatureGrid(
            onGoLearning = onGoLearning,
            onOpenChat = onOpenChat,
            onOpenGame = onOpenGame,
            onOpenMyPage = onOpenMyPage
        )
    }
}

@Composable
private fun BrandBanner() {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 간단한 로고 느낌 (이모지 대체, 필요하면 Image로 교체)
            Text(text = "🫶", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "수어지교",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "손으로 즐거움을 상상하다",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun FeatureGrid(
    onGoLearning: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenGame: () -> Unit,
    onOpenMyPage: () -> Unit
) {
    val shape: Shape = RoundedCornerShape(24.dp)

    // 카드 높이 통일 관리용 변수 추가(기본값: 130.dp)
    val cardHeight = 270.dp
    //이모지, 제목, 설명크기를 한곳에서 조절하는 변수
    val emojiSize: TextUnit = 45.sp
    val titleSize: TextUnit = 25.sp
    val subtitleSize: TextUnit = 14.sp


    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                emoji = "📚",
                title = "학습하기",
                subtitle = "기초 · 로드맵",
                onClick = onGoLearning,
                shape = shape,
                modifier = Modifier.weight(1f).height(cardHeight),
                emojiSize = emojiSize,           // [추가] 사이즈 전달
                titleSize = titleSize,           // [추가]
                subtitleSize = subtitleSize      // [추가]
            )
            FeatureCard(
                emoji = "💬",
                title = "챗봇",
                subtitle = "대화 연습",
                onClick = onOpenChat,
                shape = shape,
                modifier = Modifier.weight(1f).height(cardHeight),
                emojiSize = emojiSize,           // [추가] 사이즈 전달
                titleSize = titleSize,           // [추가]
                subtitleSize = subtitleSize      // [추가]
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                emoji = "🎮",
                title = "리듬 게임",
                subtitle = "재미있게 배우기",
                onClick = onOpenGame,
                shape = shape,
                modifier = Modifier.weight(1f).height(cardHeight),
                emojiSize = emojiSize,           // [추가] 사이즈 전달
                titleSize = titleSize,           // [추가]
                subtitleSize = subtitleSize      // [추가]
            )
            FeatureCard(
                emoji = "\uD83D\uDC64",
                title = "마이페이지",
                subtitle = "프로필 · 통계",
                onClick = onOpenMyPage,
                shape = shape,
                modifier = Modifier.weight(1f).height(cardHeight),
                emojiSize = emojiSize,           // [추가] 사이즈 전달
                titleSize = titleSize,           // [추가]
                subtitleSize = subtitleSize      // [추가]
            )
        }
    }
}

@Composable
private fun FeatureCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 32.sp,
    titleSize: TextUnit = 18.sp,
    subtitleSize: TextUnit = 13.sp
) {
    ElevatedCard(
        onClick = onClick,
        shape = shape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    ) {
        // [변경] 카드 내 콘텐츠를 가로·세로 모두 중앙 정렬로 변경
        //  - horizontalAlignment = Alignment.CenterHorizontally : 가로 중앙
        //  - verticalArrangement = Arrangement.Center : 세로 중앙
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,   // [변경] 가로 중앙 정렬
            verticalArrangement = Arrangement.Center              // [변경] 세로 중앙 정렬
        ) {
            // [변경] 이모지 크기를 살짝 키워 시각적 중심 강화(28.sp -> 32.sp)
            Text(text = emoji, fontSize = emojiSize)                  // [변경]

            Spacer(Modifier.height(12.dp))

            // [변경] 제목을 가운데 정렬(textAlign)하고, 여러 줄 대비를 위해 fillMaxWidth 적용
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = titleSize),
                textAlign = TextAlign.Center,                     // [변경] 텍스트 중앙 정렬
                modifier = Modifier.fillMaxWidth()                // [변경] 줄바꿈 시에도 중앙 유지
            )

            Spacer(Modifier.height(4.dp))

            // [변경] 부제도 가운데 정렬 + fillMaxWidth 적용
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = subtitleSize),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,                     // [변경] 텍스트 중앙 정렬
                modifier = Modifier.fillMaxWidth()                // [변경]
            )
        }
    }
}