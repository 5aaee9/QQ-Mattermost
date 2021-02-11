package me.indexyz.mattermost.utils

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import dagger.Module
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.message.data.*
import okhttp3.internal.wait
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import javax.inject.Inject

data class Attachment(
    @Json(name = "image_url")
    val imageUrl: String,
    @Json(name = "footer")
    val text: String
)

data class MattermostMessage(
    @Json(name = "username")
    val name: String,
    @Json(name = "icon_url")
    val userAvatar: String,
    @Json(name = "channel")
    val channelId: String,
    @Json(name = "text")
    val text: String,
    @Json(name = "attachments")
    val attachments: List<Attachment>
)

@Module
class MattermostClient @Inject constructor(
    private val config: BotConfig,
    private val http: HttpClient,
    private val bot: Bot
) {

    suspend fun sendMessage(message: MattermostMessage) {
        http.post<ByteArray>(config.mattermost.webhook) {
            body = Klaxon().toJsonString(message)
            contentType(ContentType.Application.Json)
        }

        println(Klaxon().toJsonString(message))
    }

    private suspend fun readImage(id: String): ByteArray {
        println("${config.mattermost.url}v4/files/${id}")
        val res = http.get<ByteArray>("${config.mattermost.url}v4/files/${id}") {
            header("Authorization", "Bearer ${config.mattermost.token}")
        }

        return res
    }

    suspend fun listenMessage() {
        this.http.wss(urlString = (config.mattermost.url + "v4/websocket").replace("https", "wss"), {
            this.cookie("MMAUTHTOKEN", config.mattermost.token)
        }) {
            while (true) {
                val frame = incoming.receive()
                val data = frame.data.decodeToString()
                println(data)
                val obj = Klaxon().parseJsonObject(StringReader(data)) ?: continue

                if (obj.string("event") != "posted") {
                    continue
                }

                val dataObj = obj.obj("data") ?: continue

                val channel = dataObj.string("channel_name") ?: continue

                val links = (config.links.filter { cfg -> cfg.channel == channel }).lastOrNull() ?: continue
                val group = bot.getGroup(links.group) ?: continue

                val post = Klaxon().parseJsonObject(
                    StringReader(dataObj.string("post") ?: continue)
                )

                if (post.obj("props")?.string("from_webhook").equals("true")) {
                    continue
                }

                val message = post.string("message") ?: continue

                var files: List<ByteArray> = emptyList()
                if (post.containsKey("file_ids")) {
                    files = post.array<String>("file_ids")!!.map {
                        readImage(it)
                    }
                }

                println(message)
                var out: MessageChain = EmptyMessageChain

                out += message
                files.map { 
                    out += group.uploadImage(ByteArrayInputStream(it))
                }

                group.sendMessage(out)
            }
        }
    }
}