package com.ssafy.a602.signup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage

/**
 * 회원가입 화면 (UI/UX)
 * - 이메일 / 닉네임 / 비밀번호 / 비밀번호 확인
 * - 약관 동의(필수) 체크박스
 * - 프로필 사진 선택(선택)
 * - 유효성 검사 & 버튼 활성화
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onSubmit: (email: String, nickname: String, password: String, photo: Uri?) -> Unit = { _,_,_,_ -> }
) {
    // 상태
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    var showPw1 by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val emailValid = remember(email) { email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) }
    val nickValid = remember(nickname) { nickname.trim().length in 2..16 }
    val pwValid = remember(password) {
        // 8자 이상, 영문/숫자/특수문자 각각 1개 이상
        password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() } &&
                password.any { !it.isLetterOrDigit() }
    }
    val pwMatch = remember(password, password2) { password.isNotEmpty() && password == password2 }

    val allValid = emailValid && nickValid && pwValid && pwMatch && agree

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> photoUri = uri }

    // 배경 그라데이션
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFFF8E9F4), Color(0xFFF3E6FF))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원가입", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { inner ->
        Column(
            modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(6.dp))

            Text(
                "환영합니다!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "수어지교에서 새로운 언어를 배워보세요",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF666666))
            )

            Spacer(Modifier.height(16.dp))

            // 프로필 사진 (선택)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB86BFF).copy(alpha = 0.2f))
                        .clickable {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Person, contentDescription = null)
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "프로필",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "프로필 사진 (선택사항)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.height(18.dp))

            // 이메일
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                label = { Text("이메일") },
                placeholder = { Text("이메일을 입력하세요") },
                singleLine = true,
                isError = email.isNotEmpty() && !emailValid
            )
            AnimatedVisibility(visible = email.isNotEmpty() && !emailValid) {
                AssistiveText("올바른 이메일 형식이 아닙니다.")
            }

            Spacer(Modifier.height(10.dp))

            // 닉네임
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                label = { Text("닉네임") },
                placeholder = { Text("닉네임을 입력하세요") },
                singleLine = true,
                isError = nickname.isNotEmpty() && !nickValid,
                supportingText = {
                    Text("2~16자", style = MaterialTheme.typography.labelSmall)
                }
            )
            AnimatedVisibility(visible = nickname.isNotEmpty() && !nickValid) {
                AssistiveText("닉네임은 2~16자여야 합니다.")
            }

            Spacer(Modifier.height(10.dp))

            // 비밀번호
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "비밀번호",
                placeholder = "비밀번호를 입력하세요",
                show = showPw1,
                onToggleShow = { showPw1 = !showPw1 },
                isError = password.isNotEmpty() && !pwValid,
                helper = "8자 이상, 영문/숫자/특수문자 조합"
            )
            AnimatedVisibility(visible = password.isNotEmpty() && !pwValid) {
                AssistiveText("조건을 만족하지 않습니다.")
            }

            Spacer(Modifier.height(10.dp))

            // 비밀번호 확인
            PasswordField(
                value = password2,
                onValueChange = { password2 = it },
                label = "비밀번호 확인",
                placeholder = "비밀번호를 다시 입력하세요",
                show = showPw2,
                onToggleShow = { showPw2 = !showPw2 },
                isError = password2.isNotEmpty() && !pwMatch
            )
            AnimatedVisibility(visible = password2.isNotEmpty() && !pwMatch) {
                AssistiveText("비밀번호가 일치하지 않습니다.")
            }

            Spacer(Modifier.height(8.dp))

            // 약관 동의
            TermsRow(
                checked = agree,
                onCheckedChange = { agree = it },
                onClickTerms = onOpenTerms,
                onClickPrivacy = onOpenPrivacy
            )

            Spacer(Modifier.height(18.dp))

            // CTA 버튼
            Button(
                onClick = { onSubmit(email.trim(), nickname.trim(), password, photoUri) },
                enabled = allValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allValid) Color(0xFFB86BFF) else Color(0xFFCCCCCC),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "회원가입",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // 로그인 링크
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val annotated = remember(primaryColor) {
                    buildAnnotatedString {
                        append("이미 계정이 있으신가요? ")
                        withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold)) {
                            append("로그인")
                        }
                    }
                }
                Text(
                    annotated,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* navigate to login route in host screen */ },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    show: Boolean,
    onToggleShow: () -> Unit,
    isError: Boolean,
    helper: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (show) "숨기기" else "보기"
                )
            }
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        supportingText = {
            if (helper != null) Text(helper, style = MaterialTheme.typography.labelSmall)
        }
    )
}

@Composable
private fun AssistiveText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    }
}

@Composable
private fun TermsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClickTerms: () -> Unit,
    onClickPrivacy: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFB86BFF),
                uncheckedColor = Color(0xFF666666)
            )
        )
        Spacer(Modifier.width(8.dp))
        val text = buildAnnotatedString {
            append("이용약관 및 ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) { append("개인정보 처리방침") }
            append("에 동의합니다.")
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 약관 동의 체크박스 토글
                onCheckedChange(!checked)
            }
        )
    }
}

// --- Preview ---
@Composable
@Preview(
    name = "기본 상태",
    showBackground = true,
    backgroundColor = 0xFFF8E9F4
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
    backgroundColor = 0xFFF8E9F4
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
    backgroundColor = 0xFFF8E9F4
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
    backgroundColor = 0xFFF8E9F4
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
@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onSubmit: (email: String, nickname: String, password: String, photo: Uri?) -> Unit = { _,_,_,_ -> }
) {
    // 상태
    var emailState by remember { mutableStateOf(email) }
    var nicknameState by remember { mutableStateOf(nickname) }
    var passwordState by remember { mutableStateOf(password) }
    var password2State by remember { mutableStateOf(password2) }
    var agreeState by remember { mutableStateOf(agree) }
    var showPw1 by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }
    var photoUriState by remember { mutableStateOf(photoUri) }

    val emailValid = remember(emailState) { emailState.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) }
    val nickValid = remember(nicknameState) { nicknameState.trim().length in 2..16 }
    val pwValid = remember(passwordState) {
        passwordState.length >= 8 &&
                passwordState.any { it.isDigit() } &&
                passwordState.any { it.isLetter() } &&
                passwordState.any { !it.isLetterOrDigit() }
    }
    val pwMatch = remember(passwordState, password2State) { passwordState.isNotEmpty() && passwordState == password2State }

    val allValid = emailValid && nickValid && pwValid && pwMatch && agreeState

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> photoUriState = uri }

    // 배경 그라데이션
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFFF8E9F4), Color(0xFFF3E6FF))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원가입", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { inner ->
        Column(
            modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(6.dp))

            Text(
                "환영합니다!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "수어배움에서 새로운 언어를 배워보세요",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF666666))
            )

            Spacer(Modifier.height(16.dp))

            // 프로필 사진 (선택)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB86BFF).copy(alpha = 0.2f))
                        .clickable {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUriState == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Person, contentDescription = null)
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = photoUriState,
                            contentDescription = "프로필",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "프로필 사진 (선택사항)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.height(18.dp))

            // 이메일
            OutlinedTextField(
                value = emailState,
                onValueChange = { emailState = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                label = { Text("이메일") },
                placeholder = { Text("이메일을 입력하세요") },
                singleLine = true,
                isError = emailState.isNotEmpty() && !emailValid
            )
            AnimatedVisibility(visible = emailState.isNotEmpty() && !emailValid) {
                AssistiveText("올바른 이메일 형식이 아닙니다.")
            }

            Spacer(Modifier.height(10.dp))

            // 닉네임
            OutlinedTextField(
                value = nicknameState,
                onValueChange = { nicknameState = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                label = { Text("닉네임") },
                placeholder = { Text("닉네임을 입력하세요") },
                singleLine = true,
                isError = nicknameState.isNotEmpty() && !nickValid,
                supportingText = {
                    Text("2~16자", style = MaterialTheme.typography.labelSmall)
                }
            )
            AnimatedVisibility(visible = nicknameState.isNotEmpty() && !nickValid) {
                AssistiveText("닉네임은 2~16자여야 합니다.")
            }

            Spacer(Modifier.height(10.dp))

            // 비밀번호
            PasswordField(
                value = passwordState,
                onValueChange = { passwordState = it },
                label = "비밀번호",
                placeholder = "비밀번호를 입력하세요",
                show = showPw1,
                onToggleShow = { showPw1 = !showPw1 },
                isError = passwordState.isNotEmpty() && !pwValid,
                helper = "8자 이상, 영문/숫자/특수문자 조합"
            )
            AnimatedVisibility(visible = passwordState.isNotEmpty() && !pwValid) {
                AssistiveText("조건을 만족하지 않습니다.")
            }

            Spacer(Modifier.height(10.dp))

            // 비밀번호 확인
            PasswordField(
                value = password2State,
                onValueChange = { password2State = it },
                label = "비밀번호 확인",
                placeholder = "비밀번호를 다시 입력하세요",
                show = showPw2,
                onToggleShow = { showPw2 = !showPw2 },
                isError = password2State.isNotEmpty() && !pwMatch
            )
            AnimatedVisibility(visible = password2State.isNotEmpty() && !pwMatch) {
                AssistiveText("비밀번호가 일치하지 않습니다.")
            }

            Spacer(Modifier.height(8.dp))

            // 약관 동의
            TermsRow(
                checked = agreeState,
                onCheckedChange = { agreeState = it },
                onClickTerms = onOpenTerms,
                onClickPrivacy = onOpenPrivacy
            )

            Spacer(Modifier.height(18.dp))

            // CTA 버튼
            Button(
                onClick = { onSubmit(emailState.trim(), nicknameState.trim(), passwordState, photoUriState) },
                enabled = allValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allValid) Color(0xFFB86BFF) else Color(0xFFCCCCCC),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "회원가입",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // 로그인 링크
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val annotated = remember(primaryColor) {
                    buildAnnotatedString {
                        append("이미 계정이 있으신가요? ")
                        withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold)) {
                            append("로그인")
                        }
                    }
                }
                Text(
                    annotated,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* navigate to login route in host screen */ },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
