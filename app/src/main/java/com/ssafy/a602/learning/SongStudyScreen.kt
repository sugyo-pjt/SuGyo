@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ssafy.a602.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.songs.SongCard      // ✅ 게임 카드 재사용 (룩앤필 통일)
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.songs.SongsViewModel

/* ─────────────────────────────────────────────────────────────
   노래 학습: 목록 화면 (게임 SongsScreen 과 동일한 톤/스타일)
   - 뒤로가기 + 타이틀은 Row로 구성 (TopAppBar 미사용)
   - 검색창과 카드 스타일은 게임 화면과 동일
   - 카드 클릭 시 onOpenDetail(songId)
   ───────────────────────────────────────────────────────────── */
@Composable
fun SongStudyListScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val vm: SongsViewModel = viewModel()
    val state by vm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFF)) // 게임 화면과 동일 배경
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 상단: 뒤로가기 + 타이틀 (게임과 같은 타이틀 톤)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = "노래 학습",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 검색창 (게임과 동일 스타일)
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("곡 검색…", color = Color(0xFF9CA3AF)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // 곡 목록
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.filtered, key = { it.id }) { song ->
                    // 게임 UI와 완전 동일하게 보여주기 위해 카드 재사용
                    SongCard(song) { onOpenDetail(song.id) }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   노래 학습: 상세 화면
   - 간단 정보 카드들로 구성 (필요 시 영상/가사/구간연습 추가)
   - 목록과 배경/톤 통일
   ───────────────────────────────────────────────────────────── */
@Composable
fun SongStudyDetailScreen(
    songId: String,
    onBack: () -> Unit
) {
    var song by remember { mutableStateOf<SongItem?>(null) }

    // 간단히 전체 목록에서 찾아오기 (실서비스면 캐시/DI 추천)
    LaunchedEffect(songId) {
        val list = GameDataManager.getSongs()
        song = list.firstOrNull { it.id == songId }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = "노래 학습",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (song == null) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 제목/아티스트
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                song!!.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                song!!.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    // 학습 가이드 (추후 영상/가사/구간연습 붙일 자리)
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "학습 가이드",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("이 곡의 주요 제스처/리듬을 단계별로 학습하세요. (영상/가이드 추가 예정)")
                        }
                    }

                    // 진행 정보
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "진행 정보",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("곡 길이: ${song!!.durationText}")
                            song!!.bestScore?.let { best ->
                                Text("내 최고 점수: ${String.format("%,d", best)}")
                            }
                        }
                    }

                    // 필요 시 학습 시작 버튼 (주석 해제해서 사용)
                    // Button(onClick = { /* 학습 세션 진입 */ }, modifier = Modifier.fillMaxWidth()) {
                    //     Text("학습 시작")
                    // }
                }
            }
        }
    }
}
