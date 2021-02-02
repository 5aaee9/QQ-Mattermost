package me.indexyz.mattermost.utils

import dagger.Module
import me.indexyz.mattermost.process.IProcess
import javax.inject.Inject
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin

@Module
class Process @Inject constructor(
    var bot: Bot,
    var processes: Map<String, @JvmSuppressWildcards IProcess>) {
    suspend fun initBot(): Unit {
        this.bot.alsoLogin()

        this.processes.forEach {
            println("Loading ${it.key}")
            it.value.register()
        }

        this.bot.join()
    }
}
