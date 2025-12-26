package com.domotics.smarthome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domotics.smarthome.notifications.NotificationViewModel
import com.domotics.smarthome.ui.auth.LoginScreen
import com.domotics.smarthome.ui.auth.RegisterScreen
import com.domotics.smarthome.ui.devices.DeviceListScreen
import com.domotics.smarthome.ui.theme.SmartDomoticsTheme
import com.domotics.smarthome.viewmodel.AuthViewModel
import com.domotics.smarthome.viewmodel.AuthViewModelFactory
import com.domotics.smarthome.viewmodel.DeviceViewModel
import com.domotics.smarthome.viewmodel.DeviceViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInClient

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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        authViewModel.handleOAuthRedirect(intent?.data)
    }
}

@Composable
fun DomoticsApp(
    authViewModel: AuthViewModel,
    deviceViewModel: DeviceViewModel,
    notificationViewModel: NotificationViewModel,
) {
    val authState by authViewModel.uiState.collectAsState()
    val navController = rememberNavController()

    val googleClient = rememberGoogleClient()
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val token = task.getResult(ApiException::class.java)?.idToken
            if (!token.isNullOrBlank()) {
                authViewModel.handleGoogleIdToken(token)
            } else {
                authViewModel.setError("Google sign-in token missing")
            }
        } catch (error: Exception) {
            authViewModel.setError(error.message ?: "Google sign-in failed")
        }
    }

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            deviceViewModel.startMqttBridge()
            navController.navigate(Routes.Devices) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Login) {
        composable(Routes.Login) {
            LoginScreen(
                state = authState,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onLogin = authViewModel::login,
                onGoogleSignIn = { googleLauncher.launch(googleClient.signInIntent) },
                onNavigateToRegister = { navController.navigate(Routes.Register) },
            )
        }

        composable(Routes.Register) {
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

        composable(Routes.Devices) {
            DeviceListScreen(
                viewModel = deviceViewModel,
                notificationViewModel = notificationViewModel,
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
                .build()
        )
    }
}

private object Routes {
    const val Login = "auth/login"
    const val Register = "auth/register"
    const val Devices = "devices"
}
