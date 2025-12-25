package com.domotics.smarthome.notifications

/**
 * Represents a structured message delivered by any external notification provider.
 */
data class NotificationMessage(
    val title: String,
    val body: String,
    val deepLink: String? = null
)

/**
 * Callback for adapters to forward incoming messages into the application layer.
 */
fun interface NotificationListener {
    fun onNotificationReceived(message: NotificationMessage)
}

/**
 * Adapter contract that allows the app to listen for notifications from different providers.
 * Implementations can wrap Firebase, a REST webhook, or any other transport without
 * changing the app logic.
 */
interface NotificationAdapter {
    fun start(listener: NotificationListener)
    fun stop()
}
