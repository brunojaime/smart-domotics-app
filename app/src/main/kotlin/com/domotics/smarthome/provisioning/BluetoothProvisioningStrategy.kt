package com.domotics.smarthome.provisioning

class BluetoothProvisioningStrategy : ProvisioningStrategy {
    override val id: String = "bluetooth_fallback"
    override val name: String = "Bluetooth Fallback"
    override val requiredUserAction: String = "Keep the device nearby and enable Bluetooth"

    override fun supports(metadata: DiscoveryMetadata): Boolean = metadata.supportsBluetoothFallback

    override suspend fun provision(
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit
    ): ProvisioningResult {
        return ProvisioningResult.Failure(
            reason = ProvisioningFailureReason.UNSUPPORTED,
            message = "Bluetooth provisioning is not yet implemented"
        )
    }

    override fun cancel() {
        // No-op for placeholder implementation
    }
}
