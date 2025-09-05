package com.ssafy.a602.navbar

import com.ssafy.a602.R

enum class BottomTab(
    val label: String,
    val iconRes: Int
) {
    SEARCH("검색", R.drawable.nav_search),
    LEARNING("학습", R.drawable.nav_learning),
    CHAT("챗봇", R.drawable.nav_chat),
    GAME("게임", R.drawable.nav_game),
    MYPAGE("마이", R.drawable.nav_mypage)
}