package com.ssafy.a602.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.camera.core.CameraSelector
import com.ssafy.a602.MainActivity

@Composable
fun GamePreparationScreen(
    song: Song,
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
    
    val bg = Color(0xFF0D1118)
    val card = Color(0xFF151B24)
    val progress = Color(0xFF8B5CF6)
    val greenBorder = Color(0xFF2BD46D)
    val error = Color(0xFFEF4444)
    
    LaunchedEffect(song) {
        viewModel.startPreparation(song)
    }
    
    // 게임 시작 콜백 설정
    LaunchedEffect(onGameStart) {
        viewModel.onGameStart = onGameStart
    }
    
    // 권한 런처 설정
    LaunchedEffect(permissionLauncher, openSettings) {
        viewModel.permissionLauncher = permissionLauncher
        viewModel.openSettings = openSettings
    }
    
    // MainActivity에 ViewModel 설정
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (context is MainActivity) {
            context.setGamePreparationViewModel(viewModel)
        }
    }
    
    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        song.title,
                        color = Color(0xFFE7ECF3),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        when (val currentState = preparationState) {
                            is GamePreparationState.LoadingResources -> "로딩 중"
                            is GamePreparationState.WaitingPermission -> "준비 중"
                            is GamePreparationState.WarmingUpCamera -> "준비 중"
                            is GamePreparationState.Countdown -> "준비 중"
                            is GamePreparationState.Ready -> "0:00"
                            is GamePreparationState.Error -> "오류"
                        },
                        color = Color(0xFFE7ECF3),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { 
                        when (val currentState = preparationState) {
                            is GamePreparationState.LoadingResources -> {
                                val resourceState = resourceLoadingState
                                if (resourceState.isComplete) 1f else 0.3f
                            }
                            is GamePreparationState.WaitingPermission -> 0.5f
                            is GamePreparationState.WarmingUpCamera -> 0.7f
                            is GamePreparationState.Countdown -> 0.9f
                            is GamePreparationState.Ready -> 1f
                            is GamePreparationState.Error -> 0f
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    trackColor = Color(0x33212535),
                    color = progress
                )

                Spacer(Modifier.height(16.dp))

                // Camera area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(card)
                        .border(3.dp, greenBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (val currentState = preparationState) {
                        is GamePreparationState.LoadingResources -> {
                            LoadingResourcesContent(
                                resourceLoadingState = resourceLoadingState,
                                progressColor = progress
                            )
                        }
                        is GamePreparationState.WaitingPermission -> {
                            // 권한 요청 UI 표시
                            PermissionRequestContent(
                                onRequestPermission = { viewModel.requestPermissions() }
                            )
                        }
                        is GamePreparationState.WarmingUpCamera -> {
                            WarmingUpContent()
                        }
                        is GamePreparationState.Countdown -> {
                            CountdownContent(countdownValue = countdownValue)
                        }
                        is GamePreparationState.Ready -> {
                            // 실제 카메라 프리뷰를 미리 보여줌 (게임과 동일한 경험)
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                lensFacing = CameraSelector.LENS_FACING_FRONT,
                                enableAnalysis = false
                            )
                        }
                        is GamePreparationState.Error -> {
                            ErrorContent(
                                message = currentState.message,
                                onRetry = { viewModel.retryPermission() },
                                onOpenSettings = { viewModel.openAppSettings() },
                                shouldShowSettingsButton = currentState.message.contains("설정")
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Lyrics area
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = card),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = when (val currentState = preparationState) {
                                is GamePreparationState.LoadingResources -> "리소스를 로딩하고 있습니다"
                                is GamePreparationState.WaitingPermission -> "권한을 확인하고 있습니다"
                                is GamePreparationState.WarmingUpCamera -> "카메라를 준비하고 있습니다"
                                is GamePreparationState.Countdown -> "게임을 시작합니다"
                                is GamePreparationState.Ready -> "어떤 길을 걸어도"
                                is GamePreparationState.Error -> "오류가 발생했습니다"
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                            },
                            label = "lyric_top"
                        ) { targetText ->
                            Text(
                                targetText,
                                color = Color(0xFF9AA3B2),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        AnimatedContent(
                            targetState = when (val currentState = preparationState) {
                                is GamePreparationState.LoadingResources -> "잠시만 기다려주세요"
                                is GamePreparationState.WaitingPermission -> "잠시만 기다려주세요"
                                is GamePreparationState.WarmingUpCamera -> "잠시만 기다려주세요"
                                is GamePreparationState.Countdown -> "곧 시작됩니다"
                                is GamePreparationState.Ready -> "열린 문을 향해 나아가"
                                is GamePreparationState.Error -> currentState.message
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                            },
                            label = "lyric_main"
                        ) { targetText ->
                            Text(
                                targetText,
                                color = Color(0xFFE7ECF3),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    // 둥근 정사각형 종료 버튼
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5A5A)),
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
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = "카메라",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "카메라 권한이 필요합니다",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "수어 인식을 위해 카메라 접근 권한을 허용해주세요",
            color = Color(0xFFB8C2D6),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "권한 허용",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun LoadingResourcesContent(
    resourceLoadingState: ResourceLoadingState,
    progressColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = progressColor,
            strokeWidth = 4.dp
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "리소스 로딩 중...",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "잠시만 기다려주세요",
            color = Color(0xFFB8C2D6),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}



@Composable
private fun WarmingUpContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF2BD46D),
            strokeWidth = 4.dp
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "카메라 준비 중",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "수어 인식을 위한 카메라를 준비하고 있습니다",
            color = Color(0xFFB8C2D6),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CountdownContent(countdownValue: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = countdownValue.toString(),
            color = Color.White,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "게임 시작까지",
            color = Color(0xFFB8C2D6),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    shouldShowSettingsButton: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "오류 발생",
            color = Color(0xFFEF4444),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = message,
            color = Color(0xFFB8C2D6),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (shouldShowSettingsButton) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "설정 열기",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(Modifier.height(12.dp))
        }
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "다시 시도",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, backgroundColor = 0xFF0D1118)
@Composable
private fun GamePreparationScreenPreview() {
    GamePreparationScreen(
        song = Song(
            id = "1",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "3:14",
            bpm = 120,
            rating = 4.5,
            bestScore = 85000
        ),
        onGameStart = {},
        onBack = {}
    )
}
