package com.domotics.smarthome.provisioning

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext

class ProvisioningException(
    val reason: ProvisioningFailureReason,
    message: String
) : Exception(message)

data class SoftApTimeouts(
    val connectTimeoutMillis: Long = 15_000,
    val credentialAckTimeoutMillis: Long = 20_000,
    val heartbeatTimeoutMillis: Long = 20_000
)

class SoftApProvisioningStrategy(
    private val timeouts: SoftApTimeouts = SoftApTimeouts()
) : ProvisioningStrategy {
    override val id: String = "soft_ap"
    override val name: String = "SoftAP"
    override val requiredUserAction: String = "Connect to the device's Wi-Fi network to continue"

    @Volatile
    private var cancelled: Boolean = false

    override fun supports(metadata: DiscoveryMetadata): Boolean = metadata.supportsSoftAp

    override suspend fun provision(
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit
    ): ProvisioningResult {
        cancelled = false
        return try {
            connectToDeviceAp(metadata, onProgress)
            sendCredentialsToDevice(metadata, credentials, onProgress)
            waitForDeviceHeartbeat(metadata, onProgress)
            ProvisioningResult.Success("${metadata.deviceSsid ?: "Device"} is joining ${credentials.ssid}")
        } catch (cancel: CancellationException) {
            ProvisioningResult.Cancelled()
        } catch (timeout: TimeoutCancellationException) {
            ProvisioningResult.Failure(
                reason = ProvisioningFailureReason.DEVICE_TIMEOUT,
                message = "Device did not respond in time"
            )
        } catch (error: ProvisioningException) {
            when (error.reason) {
                ProvisioningFailureReason.BAD_PASSWORD -> ProvisioningResult.Failure(
                    reason = ProvisioningFailureReason.BAD_PASSWORD,
                    message = "The Wi-Fi password was rejected by the device"
                )

                ProvisioningFailureReason.AP_UNREACHABLE -> ProvisioningResult.Failure(
                    reason = ProvisioningFailureReason.AP_UNREACHABLE,
                    message = "Unable to reach the device access point"
                )

                ProvisioningFailureReason.DEVICE_TIMEOUT -> ProvisioningResult.Failure(
                    reason = ProvisioningFailureReason.DEVICE_TIMEOUT,
                    message = "Device did not confirm joining the network"
                )

                ProvisioningFailureReason.CANCELLED -> ProvisioningResult.Cancelled()

                else -> ProvisioningResult.Failure(
                    reason = ProvisioningFailureReason.UNKNOWN,
                    message = error.message ?: "Unknown provisioning error"
                )
            }
        }
    }

    override fun cancel() {
        cancelled = true
    }

    private suspend fun connectToDeviceAp(
        metadata: DiscoveryMetadata,
        onProgress: (ProvisioningProgress) -> Unit
    ) {
        onProgress(ProvisioningProgress.ConnectingToDeviceAp)
        withTimeout(timeouts.connectTimeoutMillis) {
            simulateLatency()
            throwIfCancelled()
            if (!metadata.deviceApReachable) {
                throw ProvisioningException(
                    reason = ProvisioningFailureReason.AP_UNREACHABLE,
                    message = "Device access point is unreachable"
                )
            }
        }
    }

    private suspend fun sendCredentialsToDevice(
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit
    ) {
        onProgress(ProvisioningProgress.SendingCredentials)
        withTimeout(timeouts.credentialAckTimeoutMillis) {
            simulateLatency()
            throwIfCancelled()
            val expectedPassword = metadata.expectedWifiPassword
            if (expectedPassword != null && expectedPassword != credentials.password) {
                throw ProvisioningException(
                    reason = ProvisioningFailureReason.BAD_PASSWORD,
                    message = "Wi-Fi password mismatch"
                )
            }
        }
    }

    private suspend fun waitForDeviceHeartbeat(
        metadata: DiscoveryMetadata,
        onProgress: (ProvisioningProgress) -> Unit
    ) {
        onProgress(ProvisioningProgress.WaitingForDevice)
        withTimeout(timeouts.heartbeatTimeoutMillis) {
            simulateLatency()
            throwIfCancelled()
            if (!metadata.respondsToHeartbeat) {
                throw ProvisioningException(
                    reason = ProvisioningFailureReason.DEVICE_TIMEOUT,
                    message = "Device stopped responding"
                )
            }
        }
    }

    private suspend fun simulateLatency() {
        coroutineContext.ensureActive()
        delay(300)
    }

    private fun throwIfCancelled() {
        if (cancelled) {
            throw CancellationException("Provisioning cancelled")
        }
    }
}
