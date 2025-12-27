package com.domotics.smarthome.provisioning

/**
 * Contract for any provisioning strategy used to onboard a device to the user's network.
 */
interface ProvisioningStrategy {
    val id: String
    val name: String
    val requiredUserAction: String

    /**
     * Determines if this strategy can handle the given device metadata.
     */
    fun supports(metadata: DiscoveryMetadata): Boolean

    /**
     * Executes the provisioning workflow.
     */
    suspend fun provision(
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit = {}
    ): ProvisioningResult

    /**
     * Signal that provisioning should stop as soon as possible.
     */
    fun cancel()
}

fun ProvisioningStrategy.toSummary(): ProvisioningStrategySummary =
    ProvisioningStrategySummary(
        id = id,
        name = name,
        requiredUserAction = requiredUserAction
    )
