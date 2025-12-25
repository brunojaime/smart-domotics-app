package com.domotics.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domotics.smarthome.ui.theme.SmartDomoticsTheme
import com.domotics.smarthome.ui.devices.DeviceListScreen
import com.domotics.smarthome.viewmodel.DeviceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartDomoticsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DomoticsApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomoticsApp(viewModel: DeviceViewModel = viewModel()) {
    DeviceListScreen(viewModel = viewModel)
}

@Preview(showBackground = true)
@Composable
fun DomoticsAppPreview() {
    SmartDomoticsTheme {
        DomoticsApp()
    }
}
