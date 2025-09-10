package com.ssafy.a602.game.data

import com.ssafy.a602.game.Song

/**
 * 더미 데이터 제공자
 * TODO: 실제 API 연동 시 이 클래스를 API 서비스로 교체
 * 
 * ⚠️ 주의: 아래 URL들은 테스트용 무료 오디오 파일입니다.
 * 실제 API 연동 시에는 서버에서 제공하는 실제 URL을 사용해야 합니다.
 */
object FakeSongs {
    val items = listOf(
        Song(
            id = "way_back_home",
            title = "WAY BACK HOME",
            artist = "SHAUN",
            durationText = "1:02", // ERD의 song_time과 매핑
            bpm = 120,
            rating = 4.2,
            bestScore = 89650, // ERD의 rank 테이블과 매핑
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/way_back_home.jpg" // 더미 앨범 이미지 URL
        ),
        Song(
            id = "asap",
            title = "ASAP",
            artist = "STAYC",
            durationText = "2:58",
            bpm = 128,
            rating = 4.8,
            bestScore = 76420,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/asap.jpg" // 더미 앨범 이미지 URL
        ),
        Song(
            id = "hello",
            title = "안녕하세요",
            artist = "기초 인사말",
            durationText = "2:30",
            bpm = 100,
            rating = 4.5,
            bestScore = 95200,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/hello.jpg"
        ),
        Song(
            id = "dynamite",
            title = "Dynamite",
            artist = "BTS",
            durationText = "3:19",
            bpm = 114,
            rating = 4.9,
            bestScore = null,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/dynamite.jpg"
        ),
        Song(
            id = "butter",
            title = "Butter",
            artist = "BTS",
            durationText = "2:42",
            bpm = 110,
            rating = 4.7,
            bestScore = 88750,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/butter.jpg"
        ),
        Song(
            id = "permission_to_dance",
            title = "Permission to Dance",
            artist = "BTS",
            durationText = "3:07",
            bpm = 105,
            rating = 4.6,
            bestScore = 92300,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/permission_to_dance.jpg"
        ),
        Song(
            id = "life_goes_on",
            title = "Life Goes On",
            artist = "BTS",
            durationText = "3:27",
            bpm = 95,
            rating = 4.4,
            bestScore = 78900,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/life_goes_on.jpg"
        ),
        Song(
            id = "boy_with_luv",
            title = "Boy With Luv",
            artist = "BTS",
            durationText = "3:49",
            bpm = 100,
            rating = 4.8,
            bestScore = 95600,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/boy_with_luv.jpg"
        ),
        Song(
            id = "spring_day",
            title = "Spring Day",
            artist = "BTS",
            durationText = "4:34",
            bpm = 85,
            rating = 4.9,
            bestScore = 98750,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/spring_day.jpg"
        ),
        Song(
            id = "fake_love",
            title = "Fake Love",
            artist = "BTS",
            durationText = "4:02",
            bpm = 90,
            rating = 4.7,
            bestScore = 91200,
            thumbnailRes = null,
            audioUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3", // 테스트용 오디오 URL (실제 작동함)
            albumImageUrl = "https://example.com/album/fake_love.jpg"
        )
    )
}
