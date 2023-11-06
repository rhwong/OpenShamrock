@file:OptIn(DelicateCoroutinesApi::class)

package moe.fuqiuluo.shamrock.remote.service.api

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import moe.fuqiuluo.shamrock.remote.action.ActionManager
import moe.fuqiuluo.shamrock.remote.action.ActionSession
import moe.fuqiuluo.shamrock.remote.entries.EmptyObject
import moe.fuqiuluo.shamrock.remote.entries.Status
import moe.fuqiuluo.shamrock.remote.entries.resultToString
import moe.fuqiuluo.shamrock.remote.service.config.ShamrockConfig
import moe.fuqiuluo.shamrock.tools.*
import moe.fuqiuluo.shamrock.helper.Level
import moe.fuqiuluo.shamrock.helper.LogCenter
import moe.fuqiuluo.shamrock.remote.service.data.BotStatus
import moe.fuqiuluo.shamrock.remote.service.data.Self
import moe.fuqiuluo.shamrock.remote.service.data.push.MetaEventType
import moe.fuqiuluo.shamrock.remote.service.data.push.MetaSubType
import moe.fuqiuluo.shamrock.remote.service.data.push.PostType
import moe.fuqiuluo.shamrock.remote.service.data.push.PushMetaEvent
import moe.fuqiuluo.shamrock.xposed.helper.AppRuntimeFetcher
import mqq.app.MobileQQ
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import kotlin.concurrent.timer

internal abstract class WebSocketClientServlet(
    url: String,
    wsHeaders: Map<String, String>
) : BasePushServlet, WebSocketClient(URI(url), wsHeaders) {
    override fun allowPush(): Boolean {
        return ShamrockConfig.openWebSocketClient()
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        LogCenter.log("WebSocketClient onOpen: ${handshakedata?.httpStatus}, ${handshakedata?.httpStatusMessage}")
        startHeartbeatTimer()
        pushMetaLifecycle()
        GlobalPusher.register(this)
    }

    override fun onMessage(message: String) {
        GlobalScope.launch {
            handleMessage(message)
        }
    }

    private suspend fun handleMessage(message: String) {
        val respond = kotlin.runCatching {
            val actionObject = Json.parseToJsonElement(message).asJsonObject
            if (actionObject["post_type"].asStringOrNull == "meta_event") {
                // 防止二比把元事件push回来
                return
            }

            val action = actionObject["action"].asString
            val echo = actionObject["echo"] ?: EmptyJsonString
            val params = actionObject["params"].asJsonObjectOrNull ?: EmptyJsonObject

            val handler = ActionManager[action]
            handler?.handle(ActionSession(params, echo))
                ?: resultToString(
                    false,
                    Status.UnsupportedAction,
                    EmptyObject,
                    "不支持的Action",
                    echo = echo
                )
        }.getOrNull()
        respond?.let { send(it) }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        LogCenter.log("WebSocketClient onClose: $code, $reason, $remote")
        GlobalPusher.unregister(this)
    }

    override fun onError(ex: Exception?) {
        LogCenter.log("WebSocketClient onError: ${ex?.message}")
        GlobalPusher.unregister(this)
    }

    protected inline fun <reified T> pushTo(body: T) {
        if (!allowPush() || isClosed || isClosing) return
        try {
            send(GlobalJson.encodeToString(body))
        } catch (e: Throwable) {
            LogCenter.log("被动WS推送失败: ${e.stackTraceToString()}", Level.ERROR)
        }
    }

    private fun startHeartbeatTimer() {
        timer(
            name = "heartbeat",
            initialDelay = 0,
            period = 1000L * 15,
        ) {
            if (isClosed || isClosing || !isOpen) {
                cancel()
                return@timer
            }
            val runtime = AppRuntimeFetcher.appRuntime
            val curUin = runtime.currentAccountUin
            send(
                GlobalJson.encodeToString(
                    PushMetaEvent(
                        time = System.currentTimeMillis() / 1000,
                        selfId = app.longAccountUin,
                        postType = PostType.Meta,
                        type = MetaEventType.Heartbeat,
                        subType = MetaSubType.Connect,
                        status = BotStatus(
                            Self("qq", curUin.toLong()),
                            runtime.isLogin,
                            status = "正常",
                            good = true
                        ),
                        interval = 1000L * 15
                    )
                )
            )
        }
    }

    private fun pushMetaLifecycle() {
        GlobalScope.launch {
            val runtime = AppRuntimeFetcher.appRuntime
            val curUin = runtime.currentAccountUin
            pushTo(
                PushMetaEvent(
                    time = System.currentTimeMillis() / 1000,
                    selfId = app.longAccountUin,
                    postType = PostType.Meta,
                    type = MetaEventType.LifeCycle,
                    subType = MetaSubType.Connect,
                    status = BotStatus(
                        Self("qq", curUin.toLong()), runtime.isLogin, status = "正常", good = true
                    ),
                    interval = 15000
                )
            )
        }
    }
}