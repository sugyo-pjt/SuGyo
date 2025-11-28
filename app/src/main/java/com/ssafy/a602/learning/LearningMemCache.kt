package com.ssafy.a602.learning

/**
 * MVP용 임시 메모리 캐시.
 * - 앱 프로세스가 살아있는 동안만 유지됨(앱 강제종료/재시작 시 사라짐)
 * - 학습 화면에서 받은 Day 단어 목록을 저장해 두고 퀴즈 화면에서 바로 사용
 */
object LearningMemCache {

    /** 퀴즈에 필요한 최소 정보만 저장(단어 텍스트 + 영상 URL) */
    data class Item(
        val word: String,
        val videoUrl: String?
    )

    private var dayId: Int? = null
    private var items: List<Item> = emptyList()

    /** Day별 학습 단어 목록 저장 */
    fun save(day: Int, items: List<Item>) {
        this.dayId = day
        this.items = items
    }

    /** 특정 Day의 목록을 돌려줌(없으면 null) */
    fun get(day: Int): List<Item>? {
        return if (dayId == day && items.isNotEmpty()) items else null
    }

    /** 필요 시 캐시 삭제 */
    fun clear() {
        dayId = null
        items = emptyList()
    }
}
