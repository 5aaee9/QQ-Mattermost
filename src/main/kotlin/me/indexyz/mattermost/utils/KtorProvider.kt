package me.indexyz.mattermost.utils

import dagger.Module
import dagger.Provides
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import javax.inject.Singleton

@Module
class KtorProvider {
    @Provides
    @Singleton
    fun provideKtor(): HttpClient {
        return HttpClient() {
            install(WebSockets)
        }
    }
}