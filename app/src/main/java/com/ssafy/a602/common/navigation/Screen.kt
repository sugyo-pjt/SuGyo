package com.ssafy.a602.common.navigation

// 라우트들 모음
sealed class Screen(val route: String) {
    object Login            : Screen("login")
    object Home             : Screen("home")
    object Search           : Screen("search")
    object Game             : Screen("game")                 // 곡 리스트 화면
    object Chat             : Screen("chat")
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

    // ── 게임 흐름 라우트(파라미터 포함) ───────────────────────────────────────
    // NavGraph에서 사용하는 문자열과 동일하게 정의
    object GamePreparation {
        const val ARG_SONG_ID = "songId"
        const val route = "game_preparation/{$ARG_SONG_ID}"
        fun route(songId: String) = "game_preparation/$songId"
    }

    object GamePlay {
        const val ARG_SONG_ID = "songId"
        const val route = "game_play/{$ARG_SONG_ID}"
        fun route(songId: String) = "game_play/$songId"
    }

    object GameResult {
        const val ARG_SONG_ID = "songId"
        const val route = "game_result/{$ARG_SONG_ID}"
        fun route(songId: String) = "game_result/$songId"
    }

    object GameRanking {
        const val ARG_SONG_ID = "songId"
        const val route = "game_ranking/{$ARG_SONG_ID}"
        fun route(songId: String) = "game_ranking/$songId"
    }
}
