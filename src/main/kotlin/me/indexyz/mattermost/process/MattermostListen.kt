package me.indexyz.mattermost.process

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.indexyz.mattermost.utils.Attachment
import me.indexyz.mattermost.utils.BotConfig
import me.indexyz.mattermost.utils.MattermostClient
import me.indexyz.mattermost.utils.MattermostMessage
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
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

    override fun register() {
        println(config.toString())

        bot.eventChannel.subscribeAlways<GroupMessageEvent> { groupMessageEvent ->
            val links = (config.links.filter { cfg -> cfg.group == groupMessageEvent.subject.id }).lastOrNull() ?: return@subscribeAlways

            var text: String = ""
            val images: MutableList<Attachment> = mutableListOf()

            this.message.stream().forEach {
                when (it) {
                    is PlainText -> {
                        text += it.content
                    }

                    is Image -> {
                        text += "[图片]"

                        GlobalScope.launch {
                            images.add(Attachment(it.queryUrl(), "[图片]"))
                        }
                    }

                    is At -> {
                        text += it.getDisplay(this.group)
                    }

                    is AtAll -> {
                        text += "<!all>"
                    }
                }
            }

            val message = MattermostMessage(
                groupMessageEvent.senderName, groupMessageEvent.sender.avatarUrl, links.channel, text, images)

            mattermost.sendMessage(message)
        }
    }
}