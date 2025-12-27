package com.domotics.smarthome.data.device

import com.domotics.smarthome.provisioning.DiscoveryMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/** Strategy contract for running a discovery transport. */
interface DiscoveryStrategy {
    val id: String
    suspend fun discover(): List<DiscoveryFinding>
}

/**
 * Aggregates multiple [DiscoveryStrategy] instances into a lifecycle driven discovery session.
 * The UI only consumes [DiscoverySessionState] while metadata is retained internally
 * to inform provisioning later in the flow.
 */
class DiscoverySession(private val strategies: List<DiscoveryStrategy>) {
    private val metadataByDevice = mutableMapOf<String, DiscoveryMetadata>()

    fun metadataFor(deviceId: String): DiscoveryMetadata? = metadataByDevice[deviceId]

    fun discoverDevices(): Flow<DiscoverySessionState> = channelFlow {
        if (strategies.isEmpty()) {
            send(DiscoverySessionState.NoResults)
            return@channelFlow
        }

        send(DiscoverySessionState.Discovering(progress = 0))
        val aggregated = mutableListOf<DiscoveredDevice>()

        coroutineScope {
            val jobs = strategies.map { strategy ->
                async { strategy.id to strategy.discover() }
            }

            jobs.forEachIndexed { index, job ->
                try {
                    val (_, findings) = job.await()
                    findings.forEach { finding ->
                        metadataByDevice[finding.device.id] = finding.metadata
                        if (aggregated.none { it.id == finding.device.id }) {
                            aggregated += finding.device
                        }
                    }
                    val progress = ((index + 1) * 100) / strategies.size
                    send(DiscoverySessionState.Discovering(progress = progress))
                } catch (ex: Exception) {
                    send(DiscoverySessionState.Error(ex.message ?: "Discovery failed"))
                    return@coroutineScope
                }
            }
        }

        if (aggregated.isEmpty()) {
            send(DiscoverySessionState.NoResults)
        } else {
            send(DiscoverySessionState.Results(aggregated))
        }
    }
}
