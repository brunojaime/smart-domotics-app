package com.domotics.smarthome.provisioning

/**
 * Metadata discovered from the device that describes available provisioning strategies
 * and simulation flags used by different strategies.
 */
data class DiscoveryMetadata(
    val deviceSsid: String? = null,
    val supportsSoftAp: Boolean = false,
    val supportsBluetoothFallback: Boolean = false,
    val supportsOnboardingCode: Boolean = false,
    val deviceApReachable: Boolean = true,
    val expectedWifiPassword: String? = null,
    val respondsToHeartbeat: Boolean = true,
    /**
     * True when the discovery result is simulated/demo data rather than the result of
     * an actual radio scan. Use this to warn users that onboarding will not contact
     * physical hardware until real scanners are supplied.
     */
    val simulated: Boolean = false,
)

/**
 * Represents Wi-Fi credentials supplied by the user during provisioning.
 */
data class WifiCredentials(
    val ssid: String,
    val password: String
)

/**
 * High-level provisioning progress updates for UI consumption.
 */
sealed class ProvisioningProgress(val description: String) {
    data object ConnectingToDeviceAp : ProvisioningProgress("Connecting to device access point")
    data object SendingCredentials : ProvisioningProgress("Sending home Wi-Fi credentials")
    data object WaitingForDevice : ProvisioningProgress("Waiting for device to join network")
}

/**
 * Reasons why provisioning can fail.
 */
enum class ProvisioningFailureReason {
    BAD_PASSWORD,
    DEVICE_TIMEOUT,
    AP_UNREACHABLE,
    CANCELLED,
    UNSUPPORTED,
    UNKNOWN
}

sealed class ProvisioningResult {
    data class Success(val message: String) : ProvisioningResult()
    data class Failure(val reason: ProvisioningFailureReason, val message: String) : ProvisioningResult()
    data class Cancelled(val message: String = "Provisioning cancelled") : ProvisioningResult()
}

/**
 * Simplified view data for the UI to present strategy choices and current status.
 */
data class ProvisioningStrategySummary(
    val id: String,
    val name: String,
    val requiredUserAction: String
)

data class ProvisioningViewState(
    val availableStrategies: List<ProvisioningStrategySummary> = emptyList(),
    val selectedStrategy: ProvisioningStrategySummary? = null,
    val progress: ProvisioningProgress? = null,
    val lastResult: ProvisioningResult? = null
)
