package com.fluxplayer.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.fluxplayer.player.Media3PlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideMedia3PlayerManager(
        @ApplicationContext context: Context
    ): Media3PlayerManager {
        return Media3PlayerManager(context)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(playerManager: Media3PlayerManager): ExoPlayer {
        return playerManager.exoPlayer
    }
}

