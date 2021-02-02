package me.indexyz.mattermost.utils

import com.beust.klaxon.Klaxon
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Singleton

data class LinkConfig(
    val group: Long,
    val channel: String
)

data class MattermostConfig(
    val url: String,
    val token: String,
    val webhook: String,
    val selfId: String
)

data class BotConfig(
    val links: List<LinkConfig>,
    val mattermost: MattermostConfig
)

@Module
object ConfigProvider {
    @Provides
    @Singleton
    fun configFromFile(): BotConfig {
        val configFile = File("config.json")
        val configData = configFile.bufferedReader().readLines().joinToString("\n")

        return Klaxon().parse<BotConfig>(configData) ?: throw Exception("Config not valid")
    }
}