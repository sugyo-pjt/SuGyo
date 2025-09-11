package com.ssafy.a602.game.data

import com.ssafy.a602.game.songs.SongItem

/**
 * 더미 데이터 제공자
 * TODO: 실제 API 연동 시 이 클래스를 API 서비스로 교체
 * 
 * ⚠️ 주의: 아래 URL들은 테스트용 무료 오디오 파일입니다.
 * 실제 API 연동 시에는 서버에서 제공하는 실제 URL을 사용해야 합니다.
 */
object FakeSongs {
    val items = listOf(
        SongItem(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "1:02",
            bestScore = 89650,
            albumImageUrl = "https://example.com/album/way_back_home.jpg"
        ),
        SongItem(
            id = "asap",
            title = "ASAP",
            artist = "STAYC",
            durationText = "2:58",
            bestScore = 76420,
            albumImageUrl = "https://example.com/album/asap.jpg"
        ),
        SongItem(
            id = "hello",
            title = "안녕하세요",
            artist = "기초 인사말",
            durationText = "2:30",
            bestScore = 95200,
            albumImageUrl = "https://example.com/album/hello.jpg"
        ),
        SongItem(
            id = "dynamite",
            title = "Dynamite",
            artist = "BTS",
            durationText = "3:19",
            bestScore = null,
            albumImageUrl = "https://example.com/album/dynamite.jpg"
        ),
        SongItem(
            id = "butter",
            title = "Butter",
            artist = "BTS",
            durationText = "2:42",
            bestScore = 88750,
            albumImageUrl = "https://example.com/album/butter.jpg"
        ),
        SongItem(
            id = "permission_to_dance",
            title = "Permission to Dance",
            artist = "BTS",
            durationText = "3:07",
            bestScore = 92300,
            albumImageUrl = "https://example.com/album/permission_to_dance.jpg"
        ),
        SongItem(
            id = "life_goes_on",
            title = "Life Goes On",
            artist = "BTS",
            durationText = "3:27",
            bestScore = 78900,
            albumImageUrl = "https://example.com/album/life_goes_on.jpg"
        ),
        SongItem(
            id = "boy_with_luv",
            title = "Boy With Luv",
            artist = "BTS",
            durationText = "3:49",
            bestScore = 95600,
            albumImageUrl = "https://example.com/album/boy_with_luv.jpg"
        ),
        SongItem(
            id = "spring_day",
            title = "Spring Day",
            artist = "BTS",
            durationText = "4:34",
            bestScore = 98750,
            albumImageUrl = "https://example.com/album/spring_day.jpg"
        ),
        SongItem(
            id = "fake_love",
            title = "Fake Love",
            artist = "BTS",
            durationText = "4:02",
            bestScore = 91200,
            albumImageUrl = "https://example.com/album/fake_love.jpg"
        )
    )
}
