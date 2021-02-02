package me.indexyz.mattermost.utils

import dagger.Module
import dagger.Provides
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import javax.inject.Singleton

@Module
object Bot {
    @Provides
    @Singleton
    fun provideMiraiBot(): Bot {
        val username = System.getenv("BOT_USERNAME").toLong()
        val password = System.getenv("BOT_PASSWORD")!!

        return BotFactory.newBot(username, password) {
            fileBasedDeviceInfo("device.json")
        }
    }
}