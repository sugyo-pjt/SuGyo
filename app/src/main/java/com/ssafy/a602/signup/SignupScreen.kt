@file:OptIn(ExperimentalMaterial3Api::class)

package com.ssafy.a602.signup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

/* ------------ Design Tokens (목업 고정값) ------------ */
private val BgGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFF7EAF7), // very light pink
        Color(0xFFF3E7FF), // lilac tint
        Color(0xFFF5EEF9)
    )
)
private val AvatarGradient = Brush.verticalGradient(
    listOf(Color(0xFFB059FF), Color(0xFF8E3FF7))
)
private val FieldContainer = Color.White
private val FieldContent = Color(0xFF8F8F9B)
private val FieldBorder = Color(0x00FFFFFF)
private val LabelColor = Color(0xFF4A4A55)
private val Subtle = Color(0xFF7C7C88)
private val LinkBlue = Color(0xFF6E7BFF)
private val Primary = Color(0xFF8A2BE2)  // BlueViolet - 더 자주색에 가까운 색상
private val PrimaryDark = Color(0xFF6A1B9A)  // 더 진한 자주색
private val DisabledGrad = Brush.horizontalGradient(listOf(Color(0xFFE8DAFF), Color(0xFFE8DAFF)))
private val EnabledGrad = Brush.horizontalGradient(listOf(Primary, PrimaryDark))

/* ------------ Gradient Button (목업형) ------------ */
@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: ()->Unit,
    modifier: Modifier = Modifier
) {
    val grad = if (enabled) EnabledGrad else DisabledGrad
    Surface(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier
            .height(54.dp)
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .background(grad, RoundedCornerShape(16.dp))
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else Color(0xFF9C8BC8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SignUpScreen(
    onBack: () -> Unit = {},
    onPickProfile: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onSignupSuccess: () -> Unit = {},
    onLogin: () -> Unit = {},
    // ✅ 추가: 외부에서 약관 동의 완료 신호를 받기 위한 파라미터
    externalTermsAgreed: Boolean = false,
    onConsumedExternalTermsAgreed: () -> Unit = {},
    viewModel: SignupViewModel = hiltViewModel()
) {
    // ViewModel 상태 관찰
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 회원가입 성공시 콜백 호출
    LaunchedEffect(uiState.signupSuccess) {
        if (uiState.signupSuccess) {
            onSignupSuccess()
            viewModel.clearSignupSuccess()
        }
    }
    
    // state
    var email by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    var pw2 by rememberSaveable { mutableStateOf("") }
    var agree by rememberSaveable { mutableStateOf(false) }
    var showPw by rememberSaveable { mutableStateOf(false) }
    var showPw2 by rememberSaveable { mutableStateOf(false) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    
    // 약관 동의 상태 (실제 약관 ID와 동의 여부)
    var termAgreements by rememberSaveable { mutableStateOf(listOf<com.ssafy.a602.auth.dto.TermAgreement>()) }
    
    // 약관 동의 상태 관리
    var termsAgreed by rememberSaveable { mutableStateOf(false) } // 기본적으로 미동의 상태

    // ✅ 외부 신호가 true가 되면 자동 체크 + 1회성 소비
    LaunchedEffect(externalTermsAgreed) {
        if (externalTermsAgreed) {
            termsAgreed = true
            agree = true
            onConsumedExternalTermsAgreed()
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> 
        photoUri = uri
        onPickProfile()
    }

    // validation (목업엔 메시지 노출 최소화)
    val emailOk = email.isNotBlank() && email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
    val nicknameOk = nickname.trim().length >= 2
    val pwOk = pw.length >= 8 &&
        pw.any { it.isDigit() } &&
        pw.any { it.isLetter() } &&
        pw.any { !it.isLetterOrDigit() }
    val pwMatch = pw.isNotEmpty() && pw == pw2
    val canSubmit = emailOk && nicknameOk && pwOk && pwMatch && agree && termsAgreed

    Box(
        Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // 상단바 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    "회원가입", 
                    color = LabelColor, 
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // 오른쪽 공간을 위한 Spacer
                Spacer(modifier = Modifier.size(48.dp))
            }
            Text(
                "환영합니다!",
                fontSize = 26.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF111111),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "수어지교에서 새로운 언어를 배워보세요",
                color = Subtle, 
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            // Avatar + camera badge
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(AvatarGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri == null) {
                        Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    } else {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "프로필",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Surface(
                    onClick = {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "프로필 선택",
                        tint = Primary, modifier = Modifier.padding(4.dp).size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("프로필 사진 (선택사항)", color = Subtle, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))

            /* ---- Fields: filled, 둥근 모서리, 오른쪽 아이콘 ---- */
            LabeledField(
                label = "이메일",
                value = email,
                onValueChange = { email = it },
                placeholder = "이메일을 입력하세요",
                trailing = { Icon(Icons.Outlined.Email, null, tint = Subtle) },
                keyboardType = KeyboardType.Email
            )

            LabeledField(
                label = "닉네임",
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = "닉네임을 입력하세요",
                trailing = { Icon(Icons.Outlined.Person, null, tint = Subtle) },
                keyboardType = KeyboardType.Text
            )

            LabeledField(
                label = "비밀번호",
                value = pw,
                onValueChange = { pw = it },
                placeholder = "비밀번호를 입력하세요",
                trailing = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null, tint = Subtle)
                    }
                },
                keyboardType = KeyboardType.Password,
                visual = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                supporting = "8자 이상, 영문/숫자/특수문자 조합"
            )

            LabeledField(
                label = "비밀번호 확인",
                value = pw2,
                onValueChange = { pw2 = it },
                placeholder = "비밀번호를 다시 입력하세요",
                trailing = {
                    IconButton(onClick = { showPw2 = !showPw2 }) {
                        Icon(if (showPw2) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null, tint = Subtle)
                    }
                },
                keyboardType = KeyboardType.Password,
                visual = if (showPw2) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(Modifier.height(6.dp))

            // 약관 문구 (색/배치 고정)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = agree, 
                    onCheckedChange = { newValue ->
                        // 약관 동의가 완료된 경우에만 체크 가능, 체크 해제는 항상 가능
                        if (newValue && !termsAgreed) {
                            // 약관 동의가 안된 상태에서 체크하려고 하면 약관 페이지로 이동
                            onOpenTerms()
                        } else {
                            agree = newValue
                        }
                    },
                    enabled = true, // 항상 활성화 (클릭 가능)
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (termsAgreed) Primary else Color.Gray,
                        uncheckedColor = if (termsAgreed) Primary else Color.Gray,
                        disabledCheckedColor = Color.Gray,
                        disabledUncheckedColor = Color.Gray
                    )
                ) 
                Text(
                    "이용약관", 
                    color = LinkBlue, 
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { 
                        onOpenTerms()
                    }
                )
                Text("에 동의합니다.", fontSize = 14.sp, color = if (termsAgreed) Subtle else Color.Gray)
            }

            Spacer(Modifier.height(6.dp))

            // 에러 메시지 표시
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            GradientButton(
                text = if (uiState.isLoading) "회원가입 중..." else "회원가입",
                enabled = canSubmit && !uiState.isLoading,
                onClick = { 
                    // 자기소개는 현재 빈 문자열로 전달 (필요시 UI에서 입력받도록 수정)
                    val selfIntroduction = "안녕하세요! ${nickname.trim()}입니다."
                    
                    // 약관 동의 정보 생성 (현재는 단순히 체크 여부만 전달)
                    val agreements = if (agree) {
                        listOf(
                            com.ssafy.a602.auth.dto.TermAgreement(1, true), // 기본 필수 약관
                            com.ssafy.a602.auth.dto.TermAgreement(2, true)  // 기본 필수 약관
                        )
                    } else {
                        emptyList()
                    }
                    
                    viewModel.signup(email.trim(), nickname.trim(), pw, selfIntroduction, agreements)
                }
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "이미 계정이 있으신가요? ",
                    color = Subtle,
                    fontSize = 14.sp
                )
                Text(
                    "로그인",
                    color = LinkBlue,
                    modifier = Modifier.clickable { onLogin() },
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ----- Filled 라운드 입력필드 (목업 질감) ----- */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String)->Unit,
    placeholder: String,
    trailing: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType,
    visual: VisualTransformation = VisualTransformation.None,
    supporting: String? = null
) {
    Text(label, color = LabelColor, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = FieldContent) },
        trailingIcon = trailing,
        visualTransformation = visual,
//        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
//            keyboardType = keyboardType,
//            imeAction = ImeAction.Next
//        ),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldContainer,
            unfocusedContainerColor = FieldContainer,
            disabledContainerColor = FieldContainer,
            focusedIndicatorColor = FieldBorder,
            unfocusedIndicatorColor = FieldBorder,
            cursorColor = Primary,
            focusedTextColor = Color(0xFF30303A),
            unfocusedTextColor = Color(0xFF30303A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
    if (supporting != null) {
        Text(supporting, color = Subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// --- Preview ---
@Composable
@Preview(
    name = "기본 상태",
    showBackground = true,
    backgroundColor = 0xFFF7EAF7
)
private fun PreviewSignUpScreenDefault() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SignUpScreen()
    }
}

@Composable
@Preview(
    name = "입력 중 상태",
    showBackground = true,
    backgroundColor = 0xFFF7EAF7
)
private fun PreviewSignUpScreenFilling() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SignUpScreenWithState(
            email = "user@example.com",
            nickname = "사용자",
            password = "password123!",
            password2 = "password123!",
            agree = true,
            photoUri = null
        )
    }
}

@Composable
@Preview(
    name = "에러 상태",
    showBackground = true,
    backgroundColor = 0xFFF7EAF7
)
private fun PreviewSignUpScreenError() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SignUpScreenWithState(
            email = "invalid-email",
            nickname = "a", // 너무 짧음
            password = "123", // 너무 짧음
            password2 = "456", // 일치하지 않음
            agree = false,
            photoUri = null
        )
    }
}

@Composable
@Preview(
    name = "완료 가능 상태",
    showBackground = true,
    backgroundColor = 0xFFF7EAF7
)
private fun PreviewSignUpScreenReady() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SignUpScreenWithState(
            email = "user@example.com",
            nickname = "사용자닉네임",
            password = "SecurePass123!",
            password2 = "SecurePass123!",
            agree = true,
            photoUri = null
        )
    }
}

@Composable
@Preview(
    name = "다크 테마",
    showBackground = true,
    backgroundColor = 0xFF121212
)
private fun PreviewSignUpScreenDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SignUpScreen()
    }
}

// 프리뷰용 헬퍼 컴포넌트
@Composable
private fun SignUpScreenWithState(
    email: String,
    nickname: String,
    password: String,
    password2: String,
    agree: Boolean,
    photoUri: Uri?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPickProfile: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onSubmit: (email: String, nickname: String, password: String, photo: Uri?) -> Unit = { _,_,_,_ -> },
    onLogin: () -> Unit = {}
) {
    // 상태
    var emailState by remember { mutableStateOf(email) }
    var nicknameState by remember { mutableStateOf(nickname) }
    var passwordState by remember { mutableStateOf(password) }
    var password2State by remember { mutableStateOf(password2) }
    var agreeState by remember { mutableStateOf(agree) }
    var showPw by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }
    var photoUriState by remember { mutableStateOf(photoUri) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> 
        photoUriState = uri
        onPickProfile()
    }

    // validation
    val emailOk = emailState.isNotBlank() && emailState.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
    val nicknameOk = nicknameState.trim().length >= 2
    val pwOk = passwordState.length >= 8 &&
        passwordState.any { it.isDigit() } &&
        passwordState.any { it.isLetter() } &&
        passwordState.any { !it.isLetterOrDigit() }
    val pwMatch = passwordState.isNotEmpty() && passwordState == password2State
    val canSubmit = emailOk && nicknameOk && pwOk && pwMatch && agreeState

    Box(
        Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // 상단바 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    "회원가입", 
                    color = LabelColor, 
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // 오른쪽 공간을 위한 Spacer
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "환영합니다!",
                fontSize = 26.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF111111),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "수어배움에서 새로운 언어를 배워보세요",
                color = Subtle, 
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            // Avatar + camera badge
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(AvatarGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUriState == null) {
                        Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    } else {
                        AsyncImage(
                            model = photoUriState,
                            contentDescription = "프로필",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Surface(
                    onClick = {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "프로필 선택",
                        tint = Primary, modifier = Modifier.padding(4.dp).size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("프로필 사진 (선택사항)", color = Subtle, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))

            /* ---- Fields: filled, 둥근 모서리, 오른쪽 아이콘 ---- */
            LabeledField(
                label = "이메일",
                value = emailState,
                onValueChange = { emailState = it },
                placeholder = "이메일을 입력하세요",
                trailing = { Icon(Icons.Outlined.Email, null, tint = Subtle) },
                keyboardType = KeyboardType.Email
            )

            LabeledField(
                label = "닉네임",
                value = nicknameState,
                onValueChange = { nicknameState = it },
                placeholder = "닉네임을 입력하세요",
                trailing = { Icon(Icons.Outlined.Person, null, tint = Subtle) },
                keyboardType = KeyboardType.Text
            )

            LabeledField(
                label = "비밀번호",
                value = passwordState,
                onValueChange = { passwordState = it },
                placeholder = "비밀번호를 입력하세요",
                trailing = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null, tint = Subtle)
                    }
                },
                keyboardType = KeyboardType.Password,
                visual = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                supporting = "8자 이상, 영문/숫자/특수문자 조합"
            )

            LabeledField(
                label = "비밀번호 확인",
                value = password2State,
                onValueChange = { password2State = it },
                placeholder = "비밀번호를 다시 입력하세요",
                trailing = {
                    IconButton(onClick = { showPw2 = !showPw2 }) {
                        Icon(if (showPw2) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null, tint = Subtle)
                    }
                },
                keyboardType = KeyboardType.Password,
                visual = if (showPw2) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(Modifier.height(6.dp))

            // 약관 문구 (색/배치 고정)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agreeState, onCheckedChange = { agreeState = it }) 
                Text("이용약관", color = LinkBlue, fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onOpenTerms))
                Text("에 동의합니다.", fontSize = 14.sp, color = Subtle)
            }

            Spacer(Modifier.height(6.dp))

            GradientButton(
                text = "회원가입",
                enabled = canSubmit,
                onClick = { onSubmit(emailState.trim(), nicknameState.trim(), passwordState, photoUriState) }
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "이미 계정이 있으신가요? ",
                    color = Subtle,
                    fontSize = 14.sp
                )
                Text(
                    "로그인",
                    color = LinkBlue,
                    modifier = Modifier.clickable { onLogin() },
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}