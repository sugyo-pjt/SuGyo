package com.ssafy.a602.common.navigation

// 라우트들 모음
sealed class Screen(val route: String) {
    object AuthGuard : Screen("auth_guard")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Search : Screen("search")
    object Game : Screen("game")
    object Chat : Screen("chat")
    // 검색 구현하는 부분
    object SearchDetail {
        const val ARG_ID = "wordId"
        private const val BASE = "search/detail"
        const val route = "$BASE/{$ARG_ID}"
        fun route(id: Long) = "$BASE/$id"
    }
    object LearningMainPage : Screen("learning")
    object MyPage           : Screen("mypage")
    object Total_RoadMap    : Screen("learning/roadmap")

    // ── Day별 학습 상세 라우트 ────────────────────────────────────────────────
    // NavGraph에서: route = "learning/daily/{day}" / 이동: Screen.DailyStudy.route(3)
    object DailyStudy {
        const val ARG_DAY = "day"
        private const val BASE = "learning/daily"
        // 패턴(컴포저블 등록용)
        const val route = "$BASE/{$ARG_DAY}"
        // 실제 이동용
        fun route(day: Int) = "$BASE/$day"
    }

    object DailyQuiz {
        const val ARG_DAY = "day"
        private const val BASE = "learning/quiz"
        const val route = "$BASE/{$ARG_DAY}"
        fun route(day: Int) = "$BASE/$day"
    }

    // ── 게임 흐름 라우트(파라미터 포함) ───────────────────────────────────────
    // NavGraph에서 사용하는 문자열과 동일하게 정의
    object GameRanking : Screen("game_ranking")
    object GamePreparation : Screen("game_preparation")
    object GamePlay : Screen("game_play")
    object GameResult : Screen("game_result")
}