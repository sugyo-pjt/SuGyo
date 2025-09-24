package com.ssafy.a602.game.di

import com.ssafy.a602.game.play.net.WebSocketStreamer
import com.ssafy.a602.game.play.net.HttpStreamer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameModule {
    
    @Provides
    @Singleton
    fun provideWebSocketStreamer(httpStreamer: HttpStreamer): WebSocketStreamer {
        return WebSocketStreamer(httpStreamer)
    }
}
