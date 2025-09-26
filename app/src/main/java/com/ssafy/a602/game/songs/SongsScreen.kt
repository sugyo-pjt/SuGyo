package com.ssafy.a602.game.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.ssafy.a602.game.songs.SongsViewModel
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.GameMode
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.PermissionManager
import com.ssafy.a602.auth.TokenManager
import coil.compose.AsyncImage

@Composable
fun SongsScreen(
    onSongClick: (SongItem) -> Unit = {},
    permissionLauncher: ((Array<String>) -> Unit)? = null,
    openSettings: (() -> Unit)? = null
) {
    val vm: SongsViewModel = viewModel()
    val state by vm.state.collectAsState()
    val permissionState by PermissionManager.permissionState.collectAsState()
    
    // 사용자 ID 확인
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val currentUserId by remember { mutableStateOf(tokenManager.getUserId()) }
    
    // 권한 요청 후 대기 중인 노래를 저장
    var pendingSong by remember { mutableStateOf<SongItem?>(null) }
    
    // 모드 선택 다이얼로그 상태
    var showModeDialog by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    
    // 노래 클릭 시 모드 선택 다이얼로그 표시
    val handleSongClick: (SongItem) -> Unit = { song ->
        selectedSong = song
        showModeDialog = true
    }
    
    // 모드 선택 후 처리
    val handleModeSelection: (GameMode) -> Unit = { mode ->
        selectedSong?.let { song ->
            // GameDataManager에 곡과 모드 선택 저장
            GameDataManager.selectSongAndMode(song, mode)
            
            if (!permissionState.cameraGranted) {
                // 권한이 없으면 권한 요청하고 대기 중인 노래로 설정
                pendingSong = song
                permissionLauncher?.invoke(arrayOf(android.Manifest.permission.CAMERA))
            } else {
                // 권한이 있으면 바로 게임 준비 화면으로 이동
                onSongClick(song)
            }
        }
        showModeDialog = false
        selectedSong = null
    }
    
    // 권한이 허용되면 대기 중인 노래로 게임 준비 화면으로 이동
    LaunchedEffect(permissionState.cameraGranted) {
        if (permissionState.cameraGranted && pendingSong != null) {
            onSongClick(pendingSong!!)
            pendingSong = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFF))
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                "리듬게임", 
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.height(16.dp))

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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.filtered, key = { it.id }) { song ->
                    SongCard(song, onClick = handleSongClick)
                }
            }
        }
    }
    
    // 모드 선택 다이얼로그
    if (showModeDialog && selectedSong != null) {
        GameModeSelectionDialog(
            song = selectedSong!!,
            currentUserId = currentUserId,
            onModeSelected = handleModeSelection,
            onDismiss = { 
                showModeDialog = false
                selectedSong = null
            }
        )
    }
}

@Composable
fun SongCard(
    song: SongItem,
    modifier: Modifier = Modifier,
    onClick: (SongItem) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(song) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // 썸네일 (앨범 이미지 URL 사용)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.albumImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.albumImageUrl,
                        contentDescription = "앨범 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = android.R.drawable.ic_menu_gallery)
                    )
                } else {
                    Icon(
                        Icons.Outlined.PlayArrow, 
                        contentDescription = "기본 아이콘",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1F2937),
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = song.durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
                song.bestScore?.let { best ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "내 최고: ${String.format("%,d", best)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeSelectionDialog(
    song: SongItem,
    currentUserId: Long?,
    onModeSelected: (GameMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "게임 모드 선택",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "\"${song.title}\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3B82F6)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "플레이할 게임 모드를 선택해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Easy 모드 버튼
                    Button(
                        onClick = { onModeSelected(GameMode.EASY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = GameMode.EASY.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Hard 모드 버튼
                    Button(
                        onClick = { onModeSelected(GameMode.HARD) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = GameMode.HARD.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // userId가 1인 경우에만 채보만들기 버튼과 취소 버튼을 컬럼으로 묶기
                if (currentUserId == 1L) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                onModeSelected(GameMode.CHART_CREATION)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "채보만들기",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("취소")
                        }
                    }
                }
            }
        },
        dismissButton = if (currentUserId != 1L) {
            {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        } else null,
        shape = RoundedCornerShape(16.dp)
    )
}

/* ---- Preview ---- */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SongsScreenPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFF))
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "리듬게임", 
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = "", 
                    onValueChange = {}, 
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Preview에서는 빈 리스트 사용 (실제 데이터는 SongsViewModel에서 가져옴)
                    items(emptyList<SongItem>()) { SongCard(it) }
                }
            }
        }
    }
}
