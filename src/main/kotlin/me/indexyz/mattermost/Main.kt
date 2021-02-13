package me.indexyz.mattermost

import dagger.Component
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import me.indexyz.mattermost.utils.Bot
import javax.inject.Singleton
import me.indexyz.mattermost.process.MattermostProcessBindModule
import me.indexyz.mattermost.utils.ConfigProvider
import me.indexyz.mattermost.utils.KtorProvider
import me.indexyz.mattermost.utils.MattermostClient
import io.sentry.Sentry

@Singleton
@Component(modules = [
    me.indexyz.mattermost.utils.Process::class, Bot::class,
    ConfigProvider::class, KtorProvider::class, MattermostClient::class,
    MattermostProcessBindModule::class
])
interface MainComponent {
    fun process(): me.indexyz.mattermost.utils.Process
    fun mattermost(): MattermostClient
}

suspend fun main() {
    Sentry.init { options ->
        options.dsn = "https://824ebb2875524b8faaa6466db8dbdebe@o523276.ingest.sentry.io/5635483"
    }

    val main = DaggerMainComponent.builder().build()

    awaitAll(GlobalScope.async {
        main.mattermost().listenMessage()
    }, GlobalScope.async {
        main.process().initBot()
    })
}