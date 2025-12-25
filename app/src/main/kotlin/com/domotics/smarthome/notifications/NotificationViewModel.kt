package com.domotics.smarthome.notifications

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * Coordinates the notification adapter lifecycle and exposes the latest message
 * to the UI layer.
 */
class NotificationViewModel(
    private val adapter: NotificationAdapter = SimulatedNotificationAdapter()
) : ViewModel(), NotificationListener {

    private val _currentNotification = mutableStateOf<NotificationMessage?>(null)
    val currentNotification: State<NotificationMessage?> = _currentNotification

    init {
        adapter.start(this)
    }

    override fun onNotificationReceived(message: NotificationMessage) {
        _currentNotification.value = message
    }

    fun dismissNotification() {
        _currentNotification.value = null
    }

    fun simulateExternalNotification() {
        val demoMessage = NotificationMessage(
            title = "Remote alert",
            body = "An external notifier just pinged the app.",
            deepLink = "domotics://notifications/demo"
        )
        (adapter as? SimulatedNotificationAdapter)?.triggerIncomingNotification(demoMessage)
    }

    override fun onCleared() {
        super.onCleared()
        adapter.stop()
    }
}
