package me.indexyz.mattermost.process

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import kotlinx.coroutines.*
import me.indexyz.mattermost.utils.Attachment
import me.indexyz.mattermost.utils.BotConfig
import me.indexyz.mattermost.utils.MattermostClient
import me.indexyz.mattermost.utils.MattermostMessage
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import javax.inject.Inject

@Module
abstract class MattermostProcessBindModule {
    @Binds
    @IntoMap
    @StringKey("mattermost")
    internal abstract fun bindMattermostProcess(command: MattermostProcess): IProcess
}


@Module
class MattermostProcess @Inject constructor() : IProcess {
    @Inject lateinit var bot: Bot
    @Inject lateinit var config: BotConfig
    @Inject lateinit var mattermost: MattermostClient

    private fun serializeMattermostMessage(message: GroupMessageEvent): MattermostMessage? {
        val links = (config.links.filter { cfg -> cfg.group == message.subject.id }).lastOrNull() ?: return null

        var text = ""
        val images: MutableList<Attachment> = mutableListOf()

        message.message.stream().forEach {
            when (it) {
                is PlainText -> {
                    text += it.content
                }

                is Image -> {
                    text += "[图片]"

                    GlobalScope.launch {
                        images.add(Attachment(it.queryUrl()))
                    }
                }

                is FlashImage -> {
                    text += "[闪照]"

                    GlobalScope.launch {
                        images.add(Attachment(it.image.queryUrl()))
                    }
                }

                is At -> {
                    text += it.getDisplay(message.group)
                }

                is AtAll -> {
                    text += "<!all>"
                }

                else -> {
                    text += it.contentToString()
                }
            }
        }

        return MattermostMessage(
            message.senderName, message.sender.avatarUrl, links.channel, text, images)
    }


    override fun register() {
        println(config.toString())

        bot.eventChannel.subscribeAlways<GroupMessageEvent> { groupMessage ->
            val message: MattermostMessage = withTimeoutOrNull(20000) {
                serializeMattermostMessage(groupMessage)
            } ?: return@subscribeAlways


            mattermost.sendMessage(message)
        }
    }
}