package com.ssafy.a602.game.preparation

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.a602.game.CameraPreview
import com.ssafy.a602.game.songs.SongItem
import com.ssafy.a602.game.ui.GameUITheme
import com.ssafy.a602.game.ui.modern.GameProgressBar

private const val TAG = "GamePreparation/UI"

@Composable
fun GamePreparationScreen(
    song: SongItem,
    onGameStart: () -> Unit,
    onBack: () -> Unit,
    permissionLauncher: ((Array<String>) -> Unit)? = null,
    openSettings: (() -> Unit)? = null,
    viewModel: GamePreparationViewModel = viewModel()
) {
    val preparationState by viewModel.preparationState.collectAsState()
    val resourceLoadingState by viewModel.resourceLoadingState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()

    val context = LocalContext.current
    val bg = GameUITheme.Colors.DarkBackground

    // 초기화 & 로그
    LaunchedEffect(song) {
        Log.d(TAG, "startPreparation + checkPermissions")
        viewModel.checkPermissions(context)
        viewModel.startPreparation(song)
    }
    LaunchedEffect(preparationState, resourceLoadingState, permissionState) {
        Log.d(TAG, "STATE -> $preparationState | resource=$resourceLoadingState | perm=$permissionState")
    }
    LaunchedEffect(countdownValue) { Log.d(TAG, "COUNTDOWN -> $countdownValue") }
    LaunchedEffect(onGameStart) { viewModel.onGameStart = onGameStart }
    LaunchedEffect(permissionLauncher, openSettings) {
        viewModel.permissionLauncher = permissionLauncher
        viewModel.openSettings = openSettings
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    song.title,
                    color = GameUITheme.Colors.PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when (preparationState) {
                        is GamePreparationState.LoadingResources -> "로딩 중"
                        is GamePreparationState.WaitingPermission -> "준비 중"
                        is GamePreparationState.WarmingUpCamera -> "준비 중"
                        is GamePreparationState.Countdown -> "준비 중"
                        is GamePreparationState.Ready -> "0:00"
                        is GamePreparationState.Error -> "오류"
                        else -> "준비 중"
                    },
                    color = GameUITheme.Colors.SecondaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            // ✅ ProgressBar 높이 고정 (이게 없어서 아래가 0이 됐었음)
            GameProgressBar(
                progress = when (preparationState) {
                    is GamePreparationState.LoadingResources ->
                        if (resourceLoadingState.isComplete) 1f else 0.3f
                    is GamePreparationState.WaitingPermission -> 0.5f
                    is GamePreparationState.WarmingUpCamera -> 0.7f
                    is GamePreparationState.Countdown -> 0.9f
                    is GamePreparationState.Ready -> 1f
                    is GamePreparationState.Error -> 0f
                    else -> 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp) // ✅ 고정
            )

            Spacer(Modifier.height(16.dp))

            // Camera + Overlay
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .defaultMinSize(minHeight = 240.dp) // ✅ 최소 높이 보장
                    .onGloballyPositioned { Log.d(TAG, "CameraCard size=${it.size.width}x${it.size.height}") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // ① 카메라: 항상 아래
                    CameraPreview(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(0f)
                            .onGloballyPositioned {
                                Log.d(TAG, "CameraPreview placed: ${it.size.width}x${it.size.height}")
                            },
                        lensFacing = CameraSelector.LENS_FACING_FRONT,
                        enableAnalysis = false
                    )

                    // ② 오버레이: Ready 아니면 위
                    if (preparationState !is GamePreparationState.Ready) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(1f) // ✅ 카메라 위로
                                .onGloballyPositioned {
                                    Log.d(TAG, "Overlay placed: ${it.size.width}x${it.size.height}")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = Color(0xAA000000),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    when (val s = preparationState) {
                                        is GamePreparationState.LoadingResources -> {
                                            LoadingResourcesContent(
                                                resourceLoadingState = resourceLoadingState,
                                                progressColor = GameUITheme.Colors.NeonBlue
                                            )
                                        }
                                        is GamePreparationState.WaitingPermission -> {
                                            PermissionRequestContent(
                                                onRequestPermission = { viewModel.requestPermissions() }
                                            )
                                        }
                                        is GamePreparationState.WarmingUpCamera -> {
                                            WarmingUpContent()
                                        }
                                is GamePreparationState.Countdown -> {
                                    // 카운트다운 중에는 아무것도 표시하지 않음 (오버레이에서 처리)
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 빈 박스 - 카운트다운은 오버레이에서 표시
                                    }
                                }
                                        is GamePreparationState.Error -> {
                                            ErrorContent(
                                                message = s.message,
                                                onRetry = { viewModel.retryPermission() },
                                                onOpenSettings = { viewModel.openAppSettings() },
                                                shouldShowSettingsButton = s.message.contains("설정")
                                            )
                                        }
                                        else -> {
                                            LoadingResourcesContent(
                                                resourceLoadingState = resourceLoadingState,
                                                progressColor = GameUITheme.Colors.NeonBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Lyrics / 안내 영역 (원래 고정 높이라 괜찮음)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x801A1F2E)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 카운트다운 중이 아닐 때만 텍스트 표시
                    if (preparationState !is GamePreparationState.Countdown) {
                        AnimatedContent(
                            targetState = when (preparationState) {
                                is GamePreparationState.LoadingResources -> "리소스를 로딩하고 있습니다"
                                is GamePreparationState.WaitingPermission -> "권한을 확인하고 있습니다"
                                is GamePreparationState.WarmingUpCamera -> "카메라를 준비하고 있습니다"
                                is GamePreparationState.Ready -> "곧 게임이 시작됩니다"
                                is GamePreparationState.Error -> "오류가 발생했습니다"
                                else -> "준비 중입니다"
                            },
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            label = "lyric_top"
                        ) { targetText ->
                            Text(
                                targetText,
                                color = GameUITheme.Colors.SecondaryText,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        AnimatedContent(
                            targetState = when (val s = preparationState) {
                                is GamePreparationState.LoadingResources -> "잠시만 기다려주세요"
                                is GamePreparationState.WaitingPermission -> "잠시만 기다려주세요"
                                is GamePreparationState.WarmingUpCamera -> "잠시만 기다려주세요"
                                is GamePreparationState.Ready -> "잠시만 기다려주세요"
                                is GamePreparationState.Error -> s.message
                                else -> "잠시만 기다려주세요"
                            },
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            label = "lyric_main"
                        ) { targetText ->
                            Text(
                                targetText,
                                color = GameUITheme.Colors.PrimaryText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 하단 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = GameUITheme.Colors.NeonRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(80.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "종료",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "종료",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

                Spacer(Modifier.height(12.dp))
            }
            
            // 카운트다운 오버레이 - 화면 중앙에 표시
            if (preparationState is GamePreparationState.Countdown) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CountdownContent(countdownValue = countdownValue)
                }
            }
        }
    }
}

/* ---------- 상태별 컴포넌트 ---------- */

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = "카메라", tint = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("카메라 권한이 필요합니다", color = GameUITheme.Colors.PrimaryText,
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("수어 인식을 위해 카메라 접근 권한을 허용해주세요", color = GameUITheme.Colors.SecondaryText,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GameUITheme.Colors.NeonBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("권한 허용", color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun LoadingResourcesContent(
    resourceLoadingState: ResourceLoadingState,
    progressColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("로딩 중...", color = Color(0xFFFF0000), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = Color(0xFF00FF00), strokeWidth = 6.dp)
        Spacer(Modifier.height(16.dp))
        Text("리소스 로딩 중... 잠시만 기다려주세요", color = Color.White, fontSize = 16.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WarmingUpContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = GameUITheme.Colors.NeonLime, strokeWidth = 4.dp)
        Spacer(Modifier.height(16.dp))
        Text("카메라 준비 중", color = GameUITheme.Colors.PrimaryText,
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("수어 인식을 위한 카메라를 준비하고 있습니다", color = GameUITheme.Colors.SecondaryText,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}


@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    shouldShowSettingsButton: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("오류 발생", color = GameUITheme.Colors.NeonRed,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(message, color = GameUITheme.Colors.SecondaryText,
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        if (shouldShowSettingsButton) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GameUITheme.Colors.NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("설정 열기", color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GameUITheme.Colors.NeonRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("다시 시도", color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun CountdownContent(countdownValue: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdownValue.toString(),
            color = Color(0xFFFFFFFF), // 흰색
            fontSize = 200.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/* ---------- Preview ---------- */

@Preview(showBackground = true, widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0E13)
@Composable
private fun GamePreparationScreenPreview() {
    GamePreparationScreen(
        song = SongItem(
            id = "1",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bestScore = 85000,
            albumImageUrl = "https://example.com/album/way_back_home.jpg"
        ),
        onGameStart = {},
        onBack = {}
    )
}
