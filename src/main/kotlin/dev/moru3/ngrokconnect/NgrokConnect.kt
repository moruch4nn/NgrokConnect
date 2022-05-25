package dev.moru3.ngrokconnect

import com.github.alexdlaird.ngrok.NgrokClient
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig
import com.github.alexdlaird.ngrok.protocol.CreateTunnel
import com.github.alexdlaird.ngrok.protocol.Proto
import com.github.alexdlaird.ngrok.protocol.Region
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

@Plugin(id = "ngrokconnect", name = "Ngrok Connect", version = "1.0", url = "https://moru3.dev", authors = ["moru3_48"])
class NgrokConnect @Inject constructor(val proxy: ProxyServer, val logger: Logger, @DataDirectory folder: Path) {

    var isWindows = System.getProperty("os.name").startsWith("Windows")

    val folder: File = folder.toFile()

    init {
        this.folder.absoluteFile.resolve("ngrok").mkdirs()
    }

    val ngrokConfig: JavaNgrokConfig = JavaNgrokConfig.Builder()
        .withAuthToken(System.getenv("NGROK_ACCESS_TOKEN"))
        .withRegion(Region.JP)
        .withConfigPath(this.folder.absoluteFile.resolve("ngrok").resolve("ngrok.yml").toPath().also { proxy.consoleCommandSource.sendMessage(Component.text(it.toString())) })
        .withNgrokPath(this.folder.absoluteFile.resolve("ngrok").resolve(if(isWindows) "ngrok.exe" else "ngrok").toPath().also { proxy.consoleCommandSource.sendMessage(Component.text(it.toString())) })
        .build()

    val ngrokClient: NgrokClient = NgrokClient.Builder().withJavaNgrokConfig(ngrokConfig).build()

    val ngrokTunnel = CreateTunnel.Builder().withProto(Proto.TCP).withAddr(proxy.configuration.queryPort).build()

    var publicUrl: String? = null

    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    val httpClient = OkHttpClient()

    @Subscribe
    fun onInit(event: ProxyInitializeEvent) {
        connect()
    }

    fun connect() {
        publicUrl = ngrokClient.connect(ngrokTunnel).publicUrl
        proxy.consoleCommandSource.sendMessage(Component.text("ngrokにサーバーを公開しました: $publicUrl"))
        val domain: String
        val port: String
        publicUrl!!.split("//")[1].split(":").also {
            domain = it[0]
            port = it[1]
        }
        val content = "{\"data\": {\"port\":${port},\"target\":\"${domain}\"}}"
        val request = Request.Builder()
            .url("https://api.cloudflare.com/client/v4/zones/${System.getenv("CLOUDFLARE_DNS_ZONE")}/dns_records/${System.getenv("CLOUDLFARE_DNS_RECORD")}")
            .header("Authorization", "Bearer ${System.getenv("CLOUDFLARE_ACCESS_TOKEN")}")
            .header("Content-Type", "application/json")
            .patch(content.toRequestBody(JSON))
            .build()
        println(httpClient.newCall(request).execute().body?.string())
    }

    fun disconnect() {
        ngrokClient.disconnect(publicUrl)
        publicUrl = null
        proxy.consoleCommandSource.sendMessage(Component.text("ngrokを停止しました。"))
    }
}