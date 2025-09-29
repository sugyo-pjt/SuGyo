package com.ssafy.a602.term.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ssafy.a602.term.data.model.TermSummaryDto

/* ------------ Design Tokens (기존 프로젝트 컨벤션) ------------ */
private val BgGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFF7EAF7), // very light pink
        Color(0xFFF3E7FF), // lilac tint
        Color(0xFFF5EEF9)
    )
)
private val CardBackground = Color.White
private val LabelColor = Color(0xFF4A4A55)
private val Subtle = Color(0xFF7C7C88)
private val LinkBlue = Color(0xFF6E7BFF)
private val Primary = Color(0xFF8A2BE2)
private val PrimaryDark = Color(0xFF6A1B9A)
private val MandatoryBadgeColor = Color(0xFFFF6B6B)
private val OptionalBadgeColor = Color(0xFF4ECDC4)
private val EnabledGrad = Brush.horizontalGradient(listOf(Primary, PrimaryDark))
private val DisabledGrad = Brush.horizontalGradient(listOf(Color(0xFFE8DAFF), Color(0xFFE8DAFF)))

/**
 * 약관 동의 화면
 */
@Composable
fun TermsScreen(
    navController: NavController,
    viewModel: TermViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    
    // 약관 동의 상태 관리
    var allAgreed by remember { mutableStateOf(false) }
    var termAgreements by remember { mutableStateOf(mapOf<Long, Boolean>()) }
    var expandedTerms by remember { mutableStateOf(setOf<Long>()) }

    // 화면 진입 시 약관 목록 로드
    LaunchedEffect(Unit) {
        viewModel.loadSummaries()
    }
    
    // 약관 목록이 로드되면 초기 동의 상태 설정
    LaunchedEffect(state.items) {
        if (state.items.isNotEmpty()) {
            termAgreements = state.items.associate { it.id to false }
        }
    }
    
    // 전체 동의 상태 계산
    val allTermsAgreed = termAgreements.values.all { it }
    LaunchedEffect(allTermsAgreed) {
        allAgreed = allTermsAgreed
    }
    
    // 전체 동의 체크박스 변경
    val onAllAgreedChange = { agreed: Boolean ->
        allAgreed = agreed
        termAgreements = termAgreements.mapValues { agreed }
    }
    
    // 개별 약관 동의 체크박스 변경
    val onTermAgreedChange = { termId: Long, agreed: Boolean ->
        termAgreements = termAgreements + (termId to agreed)
    }
    
    // 약관 상세 내용 펼치기/접기
    val onToggleExpanded = { termId: Long ->
        expandedTerms = if (termId in expandedTerms) {
            expandedTerms - termId
        } else {
            expandedTerms + termId
        }
    }
    
    // 필수 약관 동의 상태 확인
    val mandatoryTermsAgreed = state.items
        .filter { it.mandatory }
        .all { termAgreements[it.id] == true }
    
    // 뒤로가기
    val onBack: () -> Unit = {
        navController.popBackStack()
    }
    
    // ✅ 다음(확인) 버튼 클릭 시: 이전 화면(SignUp)으로 동의 결과 전달
    val onNext: () -> Unit = {
        if (mandatoryTermsAgreed) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("TERMS_MANDATORY_AGREED", true)
        }
        navController.popBackStack()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {
            // 상단바
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
                    "약관동의",
                    color = LabelColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // 오른쪽 공간을 위한 Spacer
                Spacer(modifier = Modifier.size(48.dp))
            }

            // 본문
            when {
                state.loading -> {
                    TermsLoadingContent()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 로딩 상태에서도 확인 버튼 표시 (비활성화)
                    TermsNextButton(
                        enabled = false,
                        onClick = onNext
                    )
                }
                state.error != null -> {
                    TermsErrorContent(
                        message = state.error ?: "알 수 없는 오류가 발생했습니다",
                        onRetry = { viewModel.loadSummaries() }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 에러 상태에서도 확인 버튼 표시 (비활성화)
                    TermsNextButton(
                        enabled = false,
                        onClick = onNext
                    )
                }
                state.items.isEmpty() -> {
                    TermsEmptyContent()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 빈 상태에서도 확인 버튼 표시 (비활성화)
                    TermsNextButton(
                        enabled = false,
                        onClick = onNext
                    )
                }
                // 본문 - else 블록 내부
                else -> {
                    // 🔽 스크롤 가능한 콘텐츠 영역 (버튼 위에 고정 공간)
                    Column(
                        modifier = Modifier
                            .weight(1f)                         // 남은 높이만 차지
                            .verticalScroll(rememberScrollState())
                    ) {
                        TermsAgreementContent(
                            items = state.items,
                            allAgreed = allAgreed,
                            termAgreements = termAgreements,
                            expandedTerms = expandedTerms,
                            onAllAgreedChange = onAllAgreedChange,
                            onTermAgreedChange = onTermAgreedChange,
                            onToggleExpanded = onToggleExpanded,
                            viewModel = viewModel
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // 🔽 항상 하단에 보이는 확인 버튼
                    TermsNextButton(
                        enabled = mandatoryTermsAgreed,
                        onClick = onNext
                    )

                    // 시스템 네비게이션 바와 겹침 방지
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }

            }
        }
    }
}

/**
 * 약관 동의 컨텐츠
 */
@Composable
private fun TermsAgreementContent(
    items: List<TermSummaryDto>,
    allAgreed: Boolean,
    termAgreements: Map<Long, Boolean>,
    expandedTerms: Set<Long>,
    onAllAgreedChange: (Boolean) -> Unit,
    onTermAgreedChange: (Long, Boolean) -> Unit,
    onToggleExpanded: (Long) -> Unit,
    viewModel: TermViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // 전체 동의 체크박스
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allAgreed,
                    onCheckedChange = onAllAgreedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Primary,
                        uncheckedColor = Subtle
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "모든 약관을 확인하고 전체 동의합니다.",
                    color = LabelColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 구분선
        Divider(
            color = Color(0xFFE0E0E0),
            thickness = 1.dp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 개별 약관 동의 항목들
        items.forEach { item ->
            TermAgreementItem(
                item = item,
                isAgreed = termAgreements[item.id] ?: false,
                isExpanded = item.id in expandedTerms,
                onAgreedChange = { agreed -> onTermAgreedChange(item.id, agreed) },
                onToggleExpanded = { onToggleExpanded(item.id) },
                viewModel = viewModel
            )
            
            if (item != items.last()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * 개별 약관 동의 아이템
 */
@Composable
private fun TermAgreementItem(
    item: TermSummaryDto,
    isAgreed: Boolean,
    isExpanded: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    onToggleExpanded: () -> Unit,
    viewModel: TermViewModel
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 체크박스
            Checkbox(
                checked = isAgreed,
                onCheckedChange = onAgreedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Primary,
                    uncheckedColor = Subtle
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 약관 제목
            Text(
                text = "[${if (item.mandatory) "필수" else "선택"}] ${item.title}",
                color = LabelColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            // 내용보기 버튼
            TextButton(
                onClick = onToggleExpanded,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LinkBlue
                )
            ) {
                Text(
                    text = "내용보기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // 펼쳐진 상세 내용
        if (isExpanded) {
            TermDetailContent(
                termId = item.id,
                viewModel = viewModel,
                modifier = Modifier.padding(start = 48.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }
}

/**
 * 약관 상세 내용
 */
@Composable
private fun TermDetailContent(
    termId: Long,
    viewModel: TermViewModel,
    modifier: Modifier = Modifier
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    
    // 약관 상세 정보 로드
    LaunchedEffect(termId) {
        viewModel.loadDetail(termId)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            when {
                detailState.loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "로딩 중...",
                            color = Subtle,
                            fontSize = 12.sp
                        )
                    }
                }
                detailState.error != null -> {
                    Text(
                        text = "내용을 불러올 수 없습니다.",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
                detailState.detail != null -> {
                    val detail = detailState.detail!!
                    
                    // 약관 내용을 줄바꿈으로 분리하여 표시
                    val contentLines = detail.content.split("\n").filter { it.isNotBlank() }
                    
                    contentLines.forEach { line ->
                        if (line.startsWith("-") || line.startsWith("•")) {
                            // 불릿 포인트
                            Text(
                                text = line,
                                color = LabelColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        } else if (line.startsWith("※")) {
                            // 주의사항
                            Text(
                                text = line,
                                color = Color(0xFF666666),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            // 일반 텍스트
                            Text(
                                text = line,
                                color = LabelColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 로딩 컨텐츠
 */
@Composable
private fun TermsLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "약관을 불러오는 중...",
                color = Subtle,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 에러 컨텐츠
 */
@Composable
private fun TermsErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "오류가 발생했습니다",
                color = LabelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Subtle,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "다시 시도",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 다음 버튼
 */
@Composable
private fun TermsNextButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val grad = if (enabled) EnabledGrad else DisabledGrad
    
    Surface(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier
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
                text = "확인",
                color = if (enabled) Color.White else Color(0xFF9C8BC8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 빈 목록 컨텐츠
 */
@Composable
private fun TermsEmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "등록된 약관이 없습니다",
                color = Subtle,
                fontSize = 16.sp
            )
        }
    }
}