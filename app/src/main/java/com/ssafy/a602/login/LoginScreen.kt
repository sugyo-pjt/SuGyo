package com.ssafy.a602.login

// ────────────────────────────────────────────────────────────────────────────────
// import 영역
//  - 화면 배경/레이아웃/모양/텍스트필드/아이콘/머티리얼3/상태 등 Compose에서 쓰는 것들
//  - "뒤로가기" 아이콘은 사용하지 않으므로 filled.ArrowBack import는 제거했습니다.
// ────────────────────────────────────────────────────────────────────────────────
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*          // Column, Row, Spacer, size/width/height 등 레이아웃 관련
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*                   // Material3의 Card, Button, Text, OutlinedTextField 등
import androidx.compose.runtime.*                    // remember, mutableStateOf 등 상태 관련
import androidx.compose.ui.Alignment                // Row/Column/Box 정렬 옵션
import androidx.compose.ui.Modifier                 // Modifier 체이닝
import androidx.compose.ui.graphics.Brush          // 그라데이션(배경)
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight    // 텍스트 굵기
import androidx.compose.ui.text.input.KeyboardType // 키보드 타입(Email/Password)
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign    // 텍스트 중앙 정렬
import androidx.compose.ui.tooling.preview.Preview // 미리보기
import androidx.compose.ui.unit.dp                 // dp 단위
import androidx.compose.ui.unit.sp                 // sp 단위(폰트 크기)
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ────────────────────────────────────────────────────────────────────────────────
//  💡 아이콘이 에러라면?
//   - build.gradle.kts에 다음 의존성이 있어야 합니다:
//     implementation("androidx.compose.material:material-icons-extended")
// ────────────────────────────────────────────────────────────────────────────────


/* =============================================================================
 *  LoginScreen
 * -----------------------------------------------------------------------------
 *  - "시작 화면"을 가정하고 뒤로가기는 숨겼습니다. (TopBar 없이 중앙 제목만 표시)
 *  - 부모에서 onSubmit, onForgot, onSignup 콜백을 주입받도록 설계(재사용성↑)
 *  - 이 파일(프론트)만 수정하여 뒤로가기 제거 요구사항을 충족합니다.
 * ============================================================================= */
@Composable
fun LoginScreen(
    onBack: () -> Unit = {},                      // 시작화면이라 보통 쓰지 않음(남겨둔 건 재사용 대비)
    onLoginSuccess: (String) -> Unit = {},        // 로그인 성공시 콜백 (토큰 전달)
    onForgot: () -> Unit = {},
    onSignup: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    // ViewModel 상태 관찰
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 로그인 성공시 콜백 호출
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess && uiState.accessToken != null) {
            onLoginSuccess(uiState.accessToken!!)
            viewModel.clearLoginSuccess()
        }
    }
    
    // 1) 상단/전체 배경 그라데이션 정의 (연한 블루 → 화이트)
    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFFF1F5FF), // 매우 연한 파랑(상단)
            Color(0xFFEFF6FF), // 중간
            Color.White        // 하단 흰색
        )
    )

    // 2) 전체 컨테이너: 배경 적용 + 화면 가득 채우기
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // 3) 실제 컨텐츠를 담는 Column
        Column(
            modifier = Modifier
                .statusBarsPadding()         // 시스템 상단 영역(노치/상태바) 피하기
                .navigationBarsPadding()     // 하단 제스처 바 영역 피하기
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ───────────────────────────────────────────────────────────────────
            // [상단 영역] 뒤로가기 제거 + 중앙 제목만 표시
            //  - TopAppBar를 쓰지 않고 Text + 중앙정렬로 간단히 구성
            //  - 제목 크기는 titleMedium을 기준으로 커스터마이즈(copy)
            // ───────────────────────────────────────────────────────────────────
            Text(
                text = "로그인",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 24.sp,             // ← 글자 크기 키움
                    fontWeight = FontWeight.Bold  // ← 굵게
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center     // ← 중앙 정렬
            )

            Spacer(Modifier.height(6.dp))        // 제목과 카드 사이 간격

            // ───────────────────────────────────────────────────────────────────
            // [카드 컨테이너] 반투명 느낌의 흰 배경 카드
            //  - RoundedCornerShape(26.dp): iOS 느낌의 둥글기
            //  - elevation: 살짝 띄워 보이도록
            // ───────────────────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF)), // 0xF2 = 약 95% 불투명
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 카드 내부 패딩
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    LoginForm(
                        uiState = uiState,
                        onLogin = { email, password -> viewModel.login(email, password) },
                        onClearError = { viewModel.clearError() },
                        onForgot = onForgot,
                        onSignup = onSignup
                    )
                }
            }
        }
    }
}


/* =============================================================================
 *  Components (화면 구성 요소)
 *  - AppIcon, EmailField, PasswordField, PrimaryButton, LoginForm
 * ============================================================================= */

/**
 * 앱 심볼/로고 자리.
 * - 현재는 이모지로 대체(🤟). 실제 로고 이미지가 있다면 Image(...)로 교체하세요.
 */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(84.dp) // 원형 배지 크기
            .background(
                brush = Brush.radialGradient( // 원형 그라데이션
                    listOf(Color(0xFF4F8BFF), Color(0xFF3F51F3))
                ),
                shape = RoundedCornerShape(42.dp) // 84/2 → 완전 원형
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("🤟", style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

/**
 * 이메일 입력 필드
 * - Material3 OutlinedTextField 사용
 * - 프로젝트 Material3 버전에 따라 colors는 TextFieldDefaults.colors(...)를 사용
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // 라벨
        Text(
            "이메일",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF445069) // 진한 회색(라벨)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("이메일을 입력하세요") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),               // 입력필드 둥글기
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email            // 이메일 키보드
            ),
            // TextField 색상 커스터마이즈
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,         // 포커스 시 배경
                unfocusedContainerColor = Color.White,       // 비포커스 배경
                disabledContainerColor = Color.White,        // 비활성 배경
                focusedIndicatorColor = Color(0xFF6D7CFF),   // 포커스 테두리
                unfocusedIndicatorColor = Color(0xFFE3E8EF), // 비포커스 테두리
                disabledIndicatorColor = Color(0xFFE3E8EF),  // 비활성 테두리
                cursorColor = Color(0xFF3F51F3)              // 커서 색
            ),
            // 우측 이메일 아이콘
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "email",
                    tint = Color(0xFF98A2B3)                 // 연한 회색
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 비밀번호 입력 필드
 * - '눈 아이콘'으로 보기/가리기를 토글합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit
) {
    // 비밀번호 표시/숨김 토글 상태
    var visible by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // 라벨
        Text(
            "비밀번호",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF445069)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("비밀번호를 입력하세요") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color(0xFF6D7CFF),
                unfocusedIndicatorColor = Color(0xFFE3E8EF),
                disabledIndicatorColor = Color(0xFFE3E8EF),
                cursorColor = Color(0xFF3F51F3)
            ),
            // 입력 텍스트 표시/숨김
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            // 우측 눈 아이콘(가리기/보기 토글)
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "비밀번호 숨기기" else "비밀번호 표시",
                        tint = Color(0xFF98A2B3)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 프라이머리 버튼
 * - 로그인 버튼 전용 스타일(라운드+블루)
 */
@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F6AFB))
    ) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 로그인 폼
 * - 이메일/비밀번호 입력 상태를 내부에서 관리
 * - "로그인" 클릭 시 onLogin(email, password) 호출 → ViewModel에서 후속 처리
 */
@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
    onForgot: () -> Unit,
    onSignup: () -> Unit
) {
    // 입력 상태(간단 버전): 화면 내부에서 로컬 관리
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // 에러가 있을 때 자동으로 에러 상태 초기화
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // 에러가 있으면 잠시 후 자동으로 초기화 (선택사항)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 앱 아이콘 + 타이틀/서브타이틀
        AppIcon()
        Spacer(Modifier.height(12.dp))
        Text(
            "수어배움",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "한국수어를 쉽고 재미있게 배워보세요",
            color = Color(0xFF667085),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(20.dp))

        // 이메일/비밀번호 입력
        EmailField(email) { email = it }
        Spacer(Modifier.height(16.dp))
        PasswordField(password) { password = it }

        Spacer(Modifier.height(20.dp))

        // 에러 메시지 표시
        uiState.error?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // 로그인 버튼
        PrimaryButton(
            text = if (uiState.isLoading) "로그인 중..." else "로그인",
            onClick = {
                // 앞뒤 공백 제거 후 ViewModel로 전달
                onLogin(email.trim(), password)
            },
            enabled = !uiState.isLoading
        )

        Spacer(Modifier.height(12.dp))

        // 비밀번호 찾기/회원가입 링크(향후 라우팅 연결)
        TextButton(onClick = onForgot) {
            Text("비밀번호를 잊으셨나요?", color = Color(0xFF3F51F3))
        }

        Spacer(Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "아직 계정이 없으신가요? ",
                color = Color(0xFF667085),
                modifier = Modifier.alignByBaseline()
            )
            TextButton(
                onClick = onSignup,
                modifier = Modifier.alignByBaseline()
            ) {
                Text("회원가입", color = Color(0xFF3F51F3))
            }
        }

    }
}

/* =============================================================================
 *  Preview
 *  - Android Studio Preview에서 단독으로 미리보기할 때 사용
 * ============================================================================= */
@Preview(showBackground = true)
@Composable
private fun PreviewLogin() {
    MaterialTheme {
        LoginScreen()
    }
}
