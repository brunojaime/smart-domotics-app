package com.domotics.smarthome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domotics.smarthome.notifications.NotificationViewModel
import com.domotics.smarthome.ui.auth.LoginScreen
import com.domotics.smarthome.ui.auth.RegisterScreen
import com.domotics.smarthome.ui.devices.DeviceListScreen
import com.domotics.smarthome.ui.home.AuthGate
import com.domotics.smarthome.ui.home.CrudManagementScreen
import com.domotics.smarthome.ui.home.ProfileScreen
import com.domotics.smarthome.ui.theme.SmartDomoticsTheme
import com.domotics.smarthome.viewmodel.AppDestination
import com.domotics.smarthome.viewmodel.AppViewModel
import com.domotics.smarthome.viewmodel.AuthViewModel
import com.domotics.smarthome.viewmodel.AuthViewModelFactory
import com.domotics.smarthome.viewmodel.DeviceViewModel
import com.domotics.smarthome.viewmodel.DeviceViewModelFactory
import com.domotics.smarthome.viewmodel.ManagedSection
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.util.Log

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(applicationContext) }
    private val deviceViewModel: DeviceViewModel by viewModels { DeviceViewModelFactory(applicationContext) }
    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authViewModel.handleOAuthRedirect(intent?.data)

        setContent {
            SmartDomoticsTheme {
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DomoticsApp(
                        authViewModel = authViewModel,
                        deviceViewModel = deviceViewModel,
                        notificationViewModel = notificationViewModel,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        authViewModel.handleOAuthRedirect(intent.data)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomoticsApp(
    authViewModel: AuthViewModel,
    deviceViewModel: DeviceViewModel,
    notificationViewModel: NotificationViewModel,
    appViewModel: AppViewModel = viewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val isAuthenticated = authState.isAuthenticated
    val username = authState.name.ifBlank { authState.email }.ifBlank { "Guest" }
    val selectedDestination by appViewModel.selectedDestination.collectAsState()
    val buildings by appViewModel.buildings.collectAsState()
    val locations by appViewModel.locations.collectAsState()
    val zones by appViewModel.zones.collectAsState()
    val sensors by appViewModel.sensors.collectAsState()
    val users by appViewModel.users.collectAsState()
    val accessControls by appViewModel.accessControls.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            deviceViewModel.startMqttBridge()
        }
    }

    val drawerItems = listOf(
        DrawerEntry(AppDestination.Devices, Icons.Default.Sensors),
        DrawerEntry(AppDestination.Buildings, Icons.Default.HomeWork),
        DrawerEntry(AppDestination.Locations, Icons.Default.Route),
        DrawerEntry(AppDestination.Zones, Icons.Default.SpaceDashboard),
        DrawerEntry(AppDestination.Sensors, Icons.Default.Devices),
        DrawerEntry(AppDestination.Users, Icons.Default.People),
        DrawerEntry(AppDestination.Access, Icons.Default.Security),
        DrawerEntry(AppDestination.Profile, Icons.Default.AccountCircle),
        DrawerEntry(AppDestination.Auth, Icons.Default.Lock),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Smart Domotics",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                drawerItems.forEach { entry ->
                    NavigationDrawerItem(
                        label = { Text(entry.destination.label) },
                        selected = entry.destination == selectedDestination,
                        onClick = {
                            appViewModel.selectDestination(entry.destination)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Smart Domotics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Connected living, simplified",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            },
        ) { innerPadding ->
            when {
                selectedDestination == AppDestination.Auth -> {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AuthSection(
                            authViewModel = authViewModel,
                            onAuthenticated = { appViewModel.selectDestination(AppDestination.Devices) },
                        )
                    }
                }

                !isAuthenticated && selectedDestination.requiresAuth -> {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AuthGate { appViewModel.selectDestination(AppDestination.Auth) }
                    }
                }

                else -> {
                    when (selectedDestination) {
                        AppDestination.Devices -> DeviceListScreen(
                            viewModel = deviceViewModel,
                            notificationViewModel = notificationViewModel,
                            showTopBar = false,
                            contentPadding = innerPadding,
                        )

                        AppDestination.Buildings -> Box(modifier = Modifier.padding(innerPadding)) {
                            CrudManagementScreen(
                                section = ManagedSection.BUILDING,
                                records = buildings,
                                onSave = { id, name, description ->
                                    appViewModel.saveRecord(ManagedSection.BUILDING, id, name, description)
                                },
                                onDelete = { appViewModel.deleteRecord(ManagedSection.BUILDING, it) },
                            )
                        }

                        AppDestination.Locations -> Box(modifier = Modifier.padding(innerPadding)) {
                            CrudManagementScreen(
                                section = ManagedSection.LOCATION,
                                records = locations,
                                onSave = { id, name, description ->
                                    appViewModel.saveRecord(ManagedSection.LOCATION, id, name, description)
                                },
                                onDelete = { appViewModel.deleteRecord(ManagedSection.LOCATION, it) },
                            )
                        }

                        AppDestination.Zones -> Box(modifier = Modifier.padding(innerPadding)) {
                            CrudManagementScreen(
                                section = ManagedSection.ZONE,
                                records = zones,
                                onSave = { id, name, description ->
                                    appViewModel.saveRecord(ManagedSection.ZONE, id, name, description)
                                },
                                onDelete = { appViewModel.deleteRecord(ManagedSection.ZONE, it) },
                            )
                        }

                        AppDestination.Sensors -> Box(modifier = Modifier.padding(innerPadding)) {
                            CrudManagementScreen(
                                section = ManagedSection.SENSOR,
                                records = sensors,
                                onSave = { id, name, description ->
                                    appViewModel.saveRecord(ManagedSection.SENSOR, id, name, description)
                                },
                                onDelete = { appViewModel.deleteRecord(ManagedSection.SENSOR, it) },
                            )
                        }

                        AppDestination.Users -> Box(modifier = Modifier.padding(innerPadding)) {
                            CrudManagementScreen(
                                section = ManagedSection.USER,
                                records = users,
                                onSave = { id, name, description ->
                                    appViewModel.saveRecord(ManagedSection.USER, id, name, description)
                                },
                                onDelete = { appViewModel.deleteRecord(ManagedSection.USER, it) },
                            )
                        }

                        AppDestination.Access -> Box(modifier = Modifier.padding(innerPadding)) {
                            CrudManagementScreen(
                                section = ManagedSection.ACCESS,
                                records = accessControls,
                                onSave = { id, name, description ->
                                    appViewModel.saveRecord(ManagedSection.ACCESS, id, name, description)
                                },
                                onDelete = { appViewModel.deleteRecord(ManagedSection.ACCESS, it) },
                            )
                        }

                        AppDestination.Profile -> Box(modifier = Modifier.padding(innerPadding)) {
                            ProfileScreen(
                                username = username,
                                onLogout = {
                                    authViewModel.logout()
                                    appViewModel.selectDestination(AppDestination.Auth)
                                },
                            )
                        }

                        AppDestination.Auth -> Unit
                    }
                }
            }
        }
    }
}

private data class DrawerEntry(val destination: AppDestination, val icon: ImageVector)

@Composable
private fun AuthSection(
    authViewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val authState by authViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val googleClient = rememberGoogleClient()
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val token = task.getResult(ApiException::class.java)?.idToken
            if (!token.isNullOrBlank()) {
                Log.i("AuthGoogle", "Google sign-in returned idToken len=${token.length}")
                authViewModel.handleGoogleIdToken(token)
            } else {
                Log.w("AuthGoogle", "Google sign-in returned empty idToken")
                authViewModel.setError("Google sign-in token missing")
            }
        } catch (error: Exception) {
            Log.e("AuthGoogle", "Google sign-in failed", error)
            authViewModel.setError(error.message ?: "Google sign-in failed")
        }
    }

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            onAuthenticated()
        }
    }

    NavHost(navController = navController, startDestination = "auth/login") {
        composable("auth/login") {
            LoginScreen(
                state = authState,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onLogin = authViewModel::login,
                onGoogleSignIn = { googleLauncher.launch(googleClient.signInIntent) },
                onNavigateToRegister = { navController.navigate("auth/register") },
            )
        }
        composable("auth/register") {
            RegisterScreen(
                state = authState,
                onNameChange = authViewModel::updateName,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onRegister = authViewModel::register,
                onGoogleSignIn = { googleLauncher.launch(googleClient.signInIntent) },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun rememberGoogleClient(): GoogleSignInClient {
    val context = LocalContext.current
    return remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
                .build(),
        )
    }
}
