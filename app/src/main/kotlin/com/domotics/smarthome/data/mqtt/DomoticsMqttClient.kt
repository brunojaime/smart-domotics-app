package com.domotics.smarthome.data.mqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class DomoticsMqttClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<MqttEnvelope>(extraBufferCapacity = 32)
    val incomingMessages: SharedFlow<MqttEnvelope> = _incomingMessages.asSharedFlow()

    private var client: IMqttAsyncClient? = null

    fun connect(credentials: MqttCredentials, onReady: (() -> Unit)? = null) {
        disconnect()

        val brokerHost = if (credentials.host == "localhost") "10.0.2.2" else credentials.host
        val serverUri = "tcp://$brokerHost:${credentials.port}"

        val connectOptions = MqttConnectOptions().apply {
            userName = credentials.username
            password = credentials.password.toCharArray()
            isAutomaticReconnect = true
            isCleanSession = true
        }

        client = MqttClient(serverUri, credentials.clientId).apply {
            setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    _connectionState.value = MqttConnectionState.Connected
                    onReady?.invoke()
                }

                override fun connectionLost(cause: Throwable?) {
                    _connectionState.value = MqttConnectionState.Disconnected
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic != null && message != null) {
                        scope.launch {
                            _incomingMessages.emit(
                                MqttEnvelope(
                                    topic = topic,
                                    payload = message.payload.decodeToString(),
                                )
                            )
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttToken?) {}
            })

            _connectionState.value = MqttConnectionState.Connecting
            connect(connectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    _connectionState.value = MqttConnectionState.Connected
                    onReady?.invoke()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    _connectionState.value = MqttConnectionState.Error(exception?.message)
                }
            })
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        client?.subscribe(topic, qos, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                // No-op
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                _connectionState.value = MqttConnectionState.Error(exception?.message)
            }
        })
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        val mqttMessage = MqttMessage(payload.toByteArray()).apply {
            this.qos = qos
            isRetained = retained
        }
        client?.publish(topic, mqttMessage, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {}
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                _connectionState.value = MqttConnectionState.Error(exception?.message)
            }
        })
    }

    fun disconnect() {
        if (client?.isConnected == true) {
            client?.disconnect()
        }
        client = null
        _connectionState.value = MqttConnectionState.Disconnected
    }
}

data class MqttEnvelope(
    val topic: String,
    val payload: String,
)

data class MqttCredentials(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val clientId: String,
    val topics: List<String>,
) {
    val brokerHost: String
        get() = if (host == "localhost") "10.0.2.2" else host
}

sealed class MqttConnectionState {
    data object Disconnected : MqttConnectionState()
    data object Connecting : MqttConnectionState()
    data object Connected : MqttConnectionState()
    data class Error(val reason: String?) : MqttConnectionState()
}
