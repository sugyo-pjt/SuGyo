package com.ssafy.a602.mypage



/* ────────────────────────────────────────────────────────────────────
   Imports
   ──────────────────────────────────────────────────────────────────── */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Retrofit (실서버 모드에서만 사용됨)
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/* ────────────────────────────────────────────────────────────────────
   🔧 Backend Switch
   - true  : Fake(더미) 데이터 사용
   - false : Retrofit으로 실제 서버 호출
   ──────────────────────────────────────────────────────────────────── */
private const val USE_FAKE_BACKEND = true


/* ────────────────────────────────────────────────────────────────────
   Domain models (UI에서 쓸 데이터)
   ──────────────────────────────────────────────────────────────────── */
private data class Profile(
    val name: String,
    val email: String,
    val avatarUrl: String? = null
)

private data class Stats(
    val dayCompleted: Int,
    val streakDays: Int
)

private data class MyPageData(
    val profile: Profile,
    val stats: Stats
)

/* ────────────────────────────────────────────────────────────────────
   DataSource 인터페이스 (가짜/진짜 공통 API)
   ──────────────────────────────────────────────────────────────────── */
private interface MyPageDataSource {
    suspend fun load(): MyPageData
}

/* ────────────────────────────────────────────────────────────────────
   1) 가짜 백엔드 (더미 데이터)
   ──────────────────────────────────────────────────────────────────── */
private class FakeMyPageDataSource : MyPageDataSource {
    override suspend fun load(): MyPageData {
        delay(600) // 네트워크 흉내
        return MyPageData(
            profile = Profile(
                name = "김수어",
                email = "signlang@example.com",
                avatarUrl = null
            ),
            stats = Stats(
                dayCompleted = 12,
                streakDays = 7
            )
        )
    }
}

/* ────────────────────────────────────────────────────────────────────
   2) 진짜 백엔드 (Retrofit)
   ──────────────────────────────────────────────────────────────────── */
private interface MyPageApi {
    // 서버 스펙에 맞춰 경로/필드명만 맞추면 됩니다.
    @GET("/api/users/me")
    suspend fun getProfile(): ProfileDto

    @GET("/api/learning/stats")
    suspend fun getStats(): StatsDto
}

// 서버 응답 DTO (서버 JSON 키에 맞춰 이름 조정)
private data class ProfileDto(val name: String, val email: String, val avatarUrl: String?)
private data class StatsDto(val dayCompleted: Int, val streakDays: Int)

// Retrofit Provider (baseUrl 끝에 반드시 '/')
private object ApiProvider {
    private const val BASE_URL = "http://i13a106.p.ssafy.io:8000/"

    val api: MyPageApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MyPageApi::class.java)
    }
}

// 원격 데이터 소스
private class RemoteMyPageDataSource : MyPageDataSource {
    private val api = ApiProvider.api
    override suspend fun load(): MyPageData {
        val p = api.getProfile()
        val s = api.getStats()
        return MyPageData(
            profile = Profile(p.name, p.email, p.avatarUrl),
            stats = Stats(s.dayCompleted, s.streakDays)
        )
    }
}

/* ────────────────────────────────────────────────────────────────────
   Repository (UI가 Datasource 구체 타입을 몰라도 되게끔)
   ──────────────────────────────────────────────────────────────────── */
private class MyPageRepository(private val ds: MyPageDataSource) {
    suspend fun load(): MyPageData = ds.load()
}

/* ────────────────────────────────────────────────────────────────────
   ViewModel (+ Factory)
   ──────────────────────────────────────────────────────────────────── */
private data class MyPageUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val dayCompleted: Int = 0,
    val streakDays: Int = 0
)

private class MyPageViewModel(
    private val repo: MyPageRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MyPageUiState())
    val state: StateFlow<MyPageUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val data = repo.load()
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = data.profile.name,
                        email = data.profile.email,
                        avatarUrl = data.profile.avatarUrl,
                        dayCompleted = data.stats.dayCompleted,
                        streakDays = data.stats.streakDays
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "오류") }
            }
        }
    }
}

private class MyPageVmFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dataSource: MyPageDataSource =
            if (USE_FAKE_BACKEND) FakeMyPageDataSource() else RemoteMyPageDataSource()
        return MyPageViewModel(MyPageRepository(dataSource)) as T
    }
}

/* ────────────────────────────────────────────────────────────────────
   UI (Compose)
   ──────────────────────────────────────────────────────────────────── */
@Composable
fun MyPageScreen(
    onLogout: (() -> Unit)? = null,
    onWithdraw: (() -> Unit)? = null,
) {
    val vm: MyPageViewModel = viewModel(factory = MyPageVmFactory())
    val state by vm.state.collectAsState()

    Scaffold { inner ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(inner)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(inner)) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("오류: ${state.error}", color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.refresh() }) { Text("다시 시도") }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 상단 제목
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.elevatedCardElevation(2.dp),
                    ) {
                        Text(
                            text = "마이페이지",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    // 프로필
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE3E8FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF6C5CE7),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    state.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(state.email, color = Color(0xFF6B7280))
                            }
                        }
                    }

                    // 학습 현황
                    Text("학습 현황", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "Day ${state.dayCompleted}",
                            subtitle = "까지 학습",
                            container = Color(0xFFE9F7EF),
                            content = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "${state.streakDays}일",
                            subtitle = "연속학습",
                            container = Color(0xFFFFF3E9),
                            content = Color(0xFFF97316),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 설정
                    Text("설정", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    SettingsItem(Icons.Outlined.Lock, "비밀번호 변경") { /* TODO */ }
                    SettingsItem(Icons.Outlined.PrivacyTip, "개인정보 보호") { /* TODO */ }
                    SettingsItem(Icons.Outlined.HelpOutline, "도움말") { /* TODO */ }
                    SettingsItem(Icons.Outlined.HeadsetMic, "문의하기") { /* TODO */ }
                    SettingsItem(
                        icon = Icons.Outlined.Logout,
                        label = "로그아웃",
                        labelColor = Color(0xFFEF4444)
                    ) { onLogout?.invoke() }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "회원탈퇴",
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWithdraw?.invoke() }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* ── 재사용 컴포넌트 ─────────────────────────────────────────────── */

@Composable
private fun StatCard(
    title: String,
    subtitle: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = content
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    color = content,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(subtitle, color = content.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = labelColor)
            Spacer(Modifier.width(12.dp))
            Text(label, color = labelColor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
