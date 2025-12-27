package com.domotics.smarthome.provisioning

import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.PairingCapability
import com.domotics.smarthome.data.remote.ProvisioningApiService
import com.domotics.smarthome.data.remote.ProvisioningRequestDto
import com.domotics.smarthome.data.remote.ProvisioningStageDto
import com.domotics.smarthome.data.remote.ProvisioningStatusDto

interface ProvisioningOrchestrator {
    suspend fun provision(
        device: DiscoveredDevice,
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        strategyId: String,
        onProgress: (ProvisioningProgress) -> Unit = {},
    ): ProvisioningResult
}

class BackendProvisioningRepository(
    private val api: ProvisioningApiService,
) : ProvisioningOrchestrator {
    override suspend fun provision(
        device: DiscoveredDevice,
        metadata: DiscoveryMetadata,
        credentials: WifiCredentials,
        strategyId: String,
        onProgress: (ProvisioningProgress) -> Unit,
    ): ProvisioningResult {
        val backendStrategyId = normalizeStrategy(strategyId)
        val payload = buildPayload(credentials, metadata)
        val request = ProvisioningRequestDto(
            deviceId = device.id,
            strategy = backendStrategyId,
            deviceType = chooseDeviceType(device, metadata),
            capabilities = pairingCapabilitiesFor(device),
            payload = payload,
        )

        return runCatching {
            onProgress(ProvisioningProgress.ConnectingToDeviceAp)
            val result = api.provisionDevice(device.id, request)
            emitProgress(result.steps, onProgress)
            toUiResult(result)
        }.getOrElse { error ->
            ProvisioningResult.Failure(
                reason = ProvisioningFailureReason.UNKNOWN,
                message = error.message ?: "Provisioning failed",
            )
        }
    }

    private fun buildPayload(
        credentials: WifiCredentials,
        metadata: DiscoveryMetadata,
    ): Map<String, Any?> {
        val metadataPayload = mapOf(
            "deviceSsid" to metadata.deviceSsid,
            "supportsSoftAp" to metadata.supportsSoftAp,
            "supportsBluetoothFallback" to metadata.supportsBluetoothFallback,
            "supportsOnboardingCode" to metadata.supportsOnboardingCode,
            "deviceApReachable" to metadata.deviceApReachable,
            "expectedWifiPassword" to metadata.expectedWifiPassword,
            "respondsToHeartbeat" to metadata.respondsToHeartbeat,
            "simulated" to metadata.simulated,
        ).filterValues { it != null }

        return buildMap {
            put("ssid", credentials.ssid)
            put("password", credentials.password)
            putAll(metadataPayload)
        }
    }

    private fun chooseDeviceType(device: DiscoveredDevice, metadata: DiscoveryMetadata): String {
        if (metadata.supportsSoftAp) return "wifi"
        val firstCapability = device.pairingCapabilities.firstOrNull()
        return when (firstCapability) {
            PairingCapability.BLUETOOTH -> "bluetooth"
            PairingCapability.ONBOARDING_CODE -> "onboarding_code"
            PairingCapability.SOFT_AP -> "wifi"
            null -> "generic"
        }
    }

    private fun normalizeStrategy(strategyId: String): String = when (strategyId) {
        "soft_ap" -> "wifi"
        else -> strategyId
    }

    private fun pairingCapabilitiesFor(device: DiscoveredDevice): List<String> =
        device.pairingCapabilities.map { capability ->
            when (capability) {
                PairingCapability.SOFT_AP -> "soft_ap"
                PairingCapability.BLUETOOTH -> "bluetooth"
                PairingCapability.ONBOARDING_CODE -> "onboarding_code"
            }
        }

    private fun emitProgress(
        steps: List<com.domotics.smarthome.data.remote.ProvisioningStepDto>,
        onProgress: (ProvisioningProgress) -> Unit,
    ) {
        steps.forEach { step ->
            val progress = when (step.stage) {
                ProvisioningStageDto.DETECT -> ProvisioningProgress.ConnectingToDeviceAp
                ProvisioningStageDto.PRECHECK, ProvisioningStageDto.PROVISION -> ProvisioningProgress.SendingCredentials
                ProvisioningStageDto.CONFIRM -> ProvisioningProgress.WaitingForDevice
                else -> null
            }
            if (progress != null) {
                onProgress(progress)
            }
        }
    }

    private fun toUiResult(result: com.domotics.smarthome.data.remote.ProvisioningResultDto): ProvisioningResult {
        val failedStep = result.steps.firstOrNull { it.status == ProvisioningStatusDto.FAILED }
        val lastMessage = failedStep?.detail ?: result.steps.lastOrNull()?.detail
        val successMessage = lastMessage ?: "Provisioned via ${result.strategy}"

        return if (result.status == ProvisioningStatusDto.SUCCEEDED) {
            ProvisioningResult.Success(
                message = "$successMessage (adapter=${result.adapter})",
            )
        } else {
            ProvisioningResult.Failure(
                reason = failureReasonFrom(failedStep),
                message = lastMessage ?: "Provisioning failed",
            )
        }
    }

    private fun failureReasonFrom(step: com.domotics.smarthome.data.remote.ProvisioningStepDto?): ProvisioningFailureReason {
        val stage = step?.stage
        return when (stage) {
            ProvisioningStageDto.PRECHECK -> ProvisioningFailureReason.BAD_PASSWORD
            ProvisioningStageDto.ROLLBACK -> ProvisioningFailureReason.CANCELLED
            ProvisioningStageDto.PROVISION -> ProvisioningFailureReason.UNKNOWN
            ProvisioningStageDto.CONFIRM -> ProvisioningFailureReason.DEVICE_TIMEOUT
            else -> ProvisioningFailureReason.UNKNOWN
        }
    }
}
