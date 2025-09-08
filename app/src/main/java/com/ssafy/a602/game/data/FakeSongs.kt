package com.ssafy.a602.game.data

import com.ssafy.a602.game.Song

/**
 * 더미 데이터 제공자
 * TODO: 실제 API 연동 시 이 클래스를 API 서비스로 교체
 */
object FakeSongs {
    val items = listOf(
        Song("way_back_home","WAY BACK HOME","SHAUN","3:14",120,4.2,89650,null),
        Song("asap","ASAP","STAYC","2:58",128,4.8,76420,null),
        Song("hello","안녕하세요","기초 인사말","2:30",100,4.5,95200,null),
        Song("dynamite","Dynamite","BTS","3:19",114,4.9,null,null),
        Song("butter","Butter","BTS","2:42",110,4.7,88750,null),
        Song("permission_to_dance","Permission to Dance","BTS","3:07",105,4.6,92300,null),
        Song("life_goes_on","Life Goes On","BTS","3:27",95,4.4,78900,null),
        Song("boy_with_luv","Boy With Luv","BTS","3:49",100,4.8,95600,null),
        Song("spring_day","Spring Day","BTS","4:34",85,4.9,98750,null),
        Song("fake_love","Fake Love","BTS","4:02",90,4.7,91200,null)
    )
}
