package com.domotics.smarthome.provisioning

class OnboardingCodeProvisioningStrategy : ProvisioningStrategy {
    override val id: String = "onboarding_code"
    override val name: String = "QR / Onboarding Code"
    override val requiredUserAction: String = "Scan the onboarding code to continue"

    override fun supports(metadata: DiscoveryMetadata): Boolean = metadata.supportsOnboardingCode

    override suspend fun provision(
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        onProgress: (ProvisioningProgress) -> Unit
    ): ProvisioningResult {
        return ProvisioningResult.Failure(
            reason = ProvisioningFailureReason.UNSUPPORTED,
            message = "QR/onboarding code provisioning is not yet implemented"
        )
    }

    override fun cancel() {
        // No-op placeholder until implemented
    }
}
