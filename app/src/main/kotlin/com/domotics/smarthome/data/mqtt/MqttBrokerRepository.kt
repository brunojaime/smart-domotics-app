package com.domotics.smarthome.data.mqtt

import android.util.Log
import com.domotics.smarthome.data.auth.TokenProvider
import com.domotics.smarthome.data.remote.BrokerApiDefaults
import com.domotics.smarthome.data.remote.BrokerApiService
import com.domotics.smarthome.data.remote.MqttCredentialsDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

class MqttBrokerRepository(
    private val tokenProvider: TokenProvider? = null,
    private val api: BrokerApiService = BrokerApiService.create(BrokerApiDefaults.baseUrl, tokenProvider),
    private val mqttClient: DomoticsMqttClient = DomoticsMqttClient(),
    private val gson: Gson = Gson(),
) {
    private val _credentialsState = MutableStateFlow<MqttCredentials?>(null)
    val credentialsState: StateFlow<MqttCredentials?> = _credentialsState.asStateFlow()

    val connectionState: StateFlow<MqttConnectionState> = mqttClient.connectionState

    private var userId: String? = null

    suspend fun authenticateAndConnect(appToken: String? = null) {
        val token = appToken ?: tokenProvider?.currentToken() ?: BrokerApiDefaults.demoToken
        val bearer = "Bearer $token"
        Log.i("MqttBrokerRepository", "Requesting MQTT credentials from ${BrokerApiDefaults.baseUrl}")
        val dto = api.issueMqttCredentials(bearer)
        val credentials = dto.toDomain()
        Log.i("MqttBrokerRepository", "Received MQTT broker ${credentials.host}:${credentials.port}")
        userId = token.removePrefix("user_")
        _credentialsState.value = credentials

        mqttClient.connect(credentials) {
            subscribeToAllowedTopics(credentials, userId)
        }
    }

    private fun subscribeToAllowedTopics(credentials: MqttCredentials, currentUserId: String?) {
        credentials.topics.forEach { topic ->
            mqttClient.subscribe(topic)
        }

        // Also listen to state updates per device.
        currentUserId?.let { id ->
            val stateTopic = "users/$id/devices/+/state"
            mqttClient.subscribe(stateTopic)
        }
    }

    fun publishLightCommand(deviceId: String, isOn: Boolean, brightness: Int) {
        val id = userId ?: return
        val topic = "users/$id/devices/$deviceId/set"
        val payload = gson.toJson(LightCommandPayload(deviceId = deviceId, isOn = isOn, brightness = brightness))
        mqttClient.publish(topic, payload)
    }

    fun lightStateMessages(): Flow<LightStatePayload> =
        mqttClient.incomingMessages
            .mapNotNull { envelope ->
                kotlin.runCatching {
                    gson.fromJson(envelope.payload, LightStatePayload::class.java)
                }.getOrNull()
            }
            .flowOn(Dispatchers.IO)
}

data class LightStatePayload(
    val deviceId: String,
    val isOn: Boolean,
    val brightness: Int = 0,
)

data class LightCommandPayload(
    val deviceId: String,
    val isOn: Boolean,
    val brightness: Int,
)

private fun MqttCredentialsDto.toDomain(): MqttCredentials =
    MqttCredentials(
        host = host,
        port = port,
        username = username,
        password = password,
        clientId = clientId,
        topics = topics,
    )
