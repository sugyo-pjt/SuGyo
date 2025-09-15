package com.ssafy.a602.game.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.score.GameScoreCalculator
import com.ssafy.a602.game.score.JudgmentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val loading: Boolean = false,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val percent: Int = 0,
    val grade: String = "",
    val error: String? = null,
    val submitted: Boolean = false,
    val personalBest: Boolean = false
)

class GamePlayViewModel : ViewModel() {

    private lateinit var calc: GameScoreCalculator
    private var songId: String = ""
    private val _ui = MutableStateFlow(GameUiState())
    val ui = _ui.asStateFlow()

    fun startGame(songId: String, totalWords: Int) {
        this.songId = songId
        calc = GameScoreCalculator(songId = songId, totalWords = totalWords, baseScore = 100)
        // 초기화 후 HUD 갱신(0표시)
        _ui.value = GameUiState()
    }

    fun onServerVerdict(isPerfect: Boolean, word: String) {
        val type = if (isPerfect) JudgmentType.PERFECT else JudgmentType.MISS
        calc.addJudgment(type, word)

        // HUD용 간단 요약만 즉시 갱신
        val preview = calc.getFinal()
        _ui.value = _ui.value.copy(
            score = preview.totalScore,
            percent = preview.percent,
            grade = preview.grade,
            maxCombo = preview.maxCombo
        )
    }

    fun finishGame() {
        val req = calc.getFinal()
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            val result = GameDataManager.submitGameResult(req)
            _ui.value = result.fold(
                onSuccess = { response ->
                    _ui.value.copy(
                        loading = false,
                        submitted = true,
                        personalBest = response.isPersonalBest
                    )
                },
                onFailure = { e ->
                    _ui.value.copy(
                        loading = false,
                        error = (e.message ?: "결과 전송 실패")
                    )
                }
            )
        }
    }
}
