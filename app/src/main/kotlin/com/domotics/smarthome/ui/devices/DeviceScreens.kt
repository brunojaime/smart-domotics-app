package com.domotics.smarthome.ui.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.DiscoveryState
import com.domotics.smarthome.data.device.PairingState
import com.domotics.smarthome.data.mqtt.MqttConnectionState
import com.domotics.smarthome.notifications.NotificationViewModel
import com.domotics.smarthome.ui.theme.SmartDomoticsTheme
import com.domotics.smarthome.viewmodel.DeviceState
import com.domotics.smarthome.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceViewModel,
    notificationViewModel: NotificationViewModel,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    enableProvisioning: Boolean = false,
) {
    val devices by viewModel.devices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val showAddFlow = remember { mutableStateOf(false) }
    val notification by notificationViewModel.currentNotification
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar("Notification received: ${it.title}")
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showTopBar) {
                TopAppBar(title = { Text("Smart Domotics") })
            }
        },
        floatingActionButton = {
            if (enableProvisioning) {
                ExtendedFloatingActionButton(
                    onClick = { showAddFlow.value = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add device") },
                    text = { Text("Add device") },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            ConnectionStatusBanner(
                state = connectionState,
                onRetry = viewModel::reconnectMqtt,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                AssistChip(
                    onClick = { notificationViewModel.simulateExternalNotification() },
                    label = { Text(text = "Test notification adapter") },
                )
            }

            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices yet. Tap + to add one!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.device.id }) { device ->
                        DeviceCard(
                            deviceState = device,
                            onToggle = { viewModel.toggleDevice(device) },
                            onRemove = { viewModel.removeDevice(device) },
                        )
                    }
                }
            }
        }
    }

    if (enableProvisioning && showAddFlow.value) {
        AddDeviceFlowDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.resetDiscovery()
                showAddFlow.value = false
            },
            onPaired = {
                viewModel.resetDiscovery()
                showAddFlow.value = false
            },
        )
    }

    notification?.let { message ->
        AlertDialog(
            onDismissRequest = notificationViewModel::dismissNotification,
            title = { Text(message.title) },
            text = { Text(message.body) },
            confirmButton = {
                TextButton(onClick = notificationViewModel::dismissNotification) {
                    Text("Open app")
                }
            },
            dismissButton = {
                TextButton(onClick = notificationViewModel::dismissNotification) {
                    Text("Later")
                }
            },
        )
    }
}

@Composable
fun DeviceCard(
    deviceState: DeviceState,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(deviceState.device.name, style = MaterialTheme.typography.titleMedium)
                Text("Status: ${deviceState.status}", style = MaterialTheme.typography.bodySmall)
                Text("Brightness: ${deviceState.brightness}%", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = deviceState.isOn, onCheckedChange = { onToggle() })
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceFlowDialog(
    viewModel: DeviceViewModel,
    onDismiss: () -> Unit,
    onPaired: () -> Unit,
) {
    val discoveryState by viewModel.discoveryState.collectAsState()
    val selected by viewModel.selectedDevice.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val ssid = remember(credentials.ssid) { mutableStateOf(credentials.ssid.orEmpty()) }
    val password = remember(credentials.password) { mutableStateOf(credentials.password.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DiscoveryContent(
                    discoveryState = discoveryState,
                    selected = selected,
                    onStart = { viewModel.startDiscovery() },
                    onRetry = { viewModel.startDiscovery() },
                    onSelect = { viewModel.selectDevice(it) },
                )

                selected?.let { device ->
                    Text("Pair with ${device.name}", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = ssid.value,
                        onValueChange = {
                            ssid.value = it
                            viewModel.updateCredentials(it, password.value)
                        },
                        label = { Text("Wi‑Fi SSID") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = {
                            password.value = it
                            viewModel.updateCredentials(ssid.value, it)
                        },
                        label = { Text("Wi‑Fi password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }

                PairingStatus(pairingState = pairingState)
            }
        },
        confirmButton = {
            val enabled = selected != null && pairingState !is PairingState.Submitting
            Button(
                onClick = { viewModel.pairSelectedDevice(onPaired) },
                enabled = enabled,
            ) { Text("Pair") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DiscoveryContent(
    discoveryState: DiscoveryState,
    selected: DiscoveredDevice?,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (DiscoveredDevice) -> Unit,
) {
    when (discoveryState) {
        DiscoveryState.Idle -> {
            OutlinedButton(onClick = onStart) { Text("Scan for devices") }
        }

        is DiscoveryState.Scanning -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(progress = discoveryState.progress / 100f, modifier = Modifier.fillMaxWidth())
                Text("Scanning... ${discoveryState.progress}%")
            }
        }

        is DiscoveryState.Results -> {
            Text("Select a device", style = MaterialTheme.typography.titleSmall)
            discoveryState.devices.forEach { device ->
                val isSelected = selected?.id == device.id
                val border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null
                OutlinedButton(
                    onClick = { onSelect(device) },
                    modifier = Modifier.fillMaxWidth(),
                    border = border,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                        if (device.capabilities.isNotEmpty()) {
                            Text(device.capabilities.joinToString(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        DiscoveryState.NoResults -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No devices found", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }

        is DiscoveryState.Error -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: ${discoveryState.message}")
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun PairingStatus(pairingState: PairingState) {
    when (pairingState) {
        PairingState.Idle -> {}
        PairingState.Submitting -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending credentials...")
            }
        }

        is PairingState.Success -> Text("Paired with ${pairingState.device.name}")
        is PairingState.Failure -> Text("Failed: ${pairingState.reason}")
    }
}

@Composable
private fun ConnectionStatusBanner(
    state: MqttConnectionState,
    onRetry: () -> Unit,
) {
    val message = when (state) {
        MqttConnectionState.Connected -> "MQTT connected"
        MqttConnectionState.Connecting -> "MQTT connecting..."
        is MqttConnectionState.Error -> "MQTT error: ${state.reason ?: "unknown"}"
        MqttConnectionState.Disconnected -> "MQTT disconnected"
    }

    val canRetry = when (state) {
        MqttConnectionState.Connected, MqttConnectionState.Connecting -> false
        is MqttConnectionState.Error, MqttConnectionState.Disconnected -> true
    }

    AssistChip(
        onClick = { if (canRetry) onRetry() },
        label = { Text(message) },
        enabled = true,
    )
}

@Preview
@Composable
private fun DeviceListPreview() {
    SmartDomoticsTheme {
        val deviceViewModel: DeviceViewModel = viewModel()
        val notificationViewModel: NotificationViewModel = viewModel()
        DeviceListScreen(deviceViewModel, notificationViewModel)
    }
}
