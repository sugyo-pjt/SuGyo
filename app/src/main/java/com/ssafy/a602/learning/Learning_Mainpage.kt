package com.ssafy.a602.learning

// 앱 리소스 R 사용 (android.R 말고, 앱의 R!)
import com.ssafy.a602.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 학습 메인 화면
 * @param onStartRoadmap  "로드맵 시작하기" 버튼 클릭 시 NavGraph에서 넘어오는 콜백(→ Roadmap 화면으로 이동)
 * @param progressDay     현재 학습 진도(가짜 값). 백엔드 연동 시 서버 응답으로 교체하면 됨.
 */
@Composable
fun LearningMainPage(
    onStartRoadmap: () -> Unit = {},
    progressDay: Int = 5
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 상단 헤더: 아이콘 + "학습하기" ───────────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [이미지 사용] res/drawable/ic_header_learning.(png|webp|xml)
                Image(
                    painter = painterResource(id = R.drawable.study),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "학습하기",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // ── 섹션 타이틀 ─────────────────────────────────────────────────────
        Column {
            Text(
                text = "학습 선택",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "원하는 학습 방식을 선택해주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // ── 카드 1: 노래 학습(자음/모음) ─────────────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // [이미지 사용] res/drawable/illust_learning_books.*
                Image(
                    painter = painterResource(id = R.drawable.study),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "노래 학습 (자음/모음)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "기본기부터 차근차근 익혀보세요",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ── 카드 2: 로드맵 + 진행 현황 + 버튼 ──────────────────────────────
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 상단: 아이콘(원 배경) + 타이틀/부제
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        // [이미지 사용] res/drawable/ic_roadmap.*
                        Image(
                            painter = painterResource(id = R.drawable.analysis),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "로드맵",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "체계적인 단계별 학습",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // 진행 현황 박스 (연한 그린 톤)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(16.dp))
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "현재 학습 진도",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            // [가짜 값 표시] progressDay를 서버 값으로 바꾸면 됨
                            text = "Day $progressDay",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16A34A)
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "까지 완료했습니다",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                // 하단: "로드맵 시작하기" 버튼
                Button(
                    onClick = onStartRoadmap, // [네비게이션 연결 지점] NavGraph에서 route 이동 연결
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    // [이미지 사용] 왼쪽 아이콘: res/drawable/ic_roadmap_start.*
                    Image(
                        painter = painterResource(id = R.drawable.analysis),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "로드맵 시작하기")
                    Spacer(Modifier.weight(1f))
                    // [이미지 사용] 오른쪽 화살표: res/drawable/ic_arrow_right.*
                    Image(
                        painter = painterResource(id = R.drawable.analysis),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
