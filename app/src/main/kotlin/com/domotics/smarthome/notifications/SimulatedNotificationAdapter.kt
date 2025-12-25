package com.domotics.smarthome.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple adapter that mimics an external provider. It allows us to plug in a
 * production-ready provider later (e.g., Firebase) while testing the UI and
 * domain flow today.
 */
class SimulatedNotificationAdapter : NotificationAdapter {
    private var listener: NotificationListener? = null
    private var job: Job? = null

    override fun start(listener: NotificationListener) {
        this.listener = listener
    }

    override fun stop() {
        job?.cancel()
        listener = null
    }

    /**
     * Emit a notification after a brief delay to simulate network/transport latency.
     */
    fun triggerIncomingNotification(message: NotificationMessage) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Default).launch {
            delay(300L)
            listener?.onNotificationReceived(message)
        }
    }
}
