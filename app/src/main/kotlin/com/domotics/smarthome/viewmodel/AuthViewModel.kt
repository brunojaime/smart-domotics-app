package com.domotics.smarthome.viewmodel

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.domotics.smarthome.BuildConfig
import com.domotics.smarthome.data.auth.AuthRepository
import com.domotics.smarthome.data.auth.AuthTokens
import com.domotics.smarthome.data.auth.AuthApiService
import com.domotics.smarthome.data.auth.SecureTokenStorage
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val tokenStorage: SecureTokenStorage,
) : ViewModel() {
    private val logTag = "AuthGoogle"
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStorage.read()?.let { tokens ->
                _uiState.update { it.copy(isAuthenticated = true) }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { current ->
            current.copy(
                name = value,
                nameError = if (value.isBlank()) "Name is required" else null,
                errorMessage = null,
            )
        }
    }

    fun updateEmail(value: String) {
        _uiState.update { current ->
            current.copy(
                email = value,
                emailError = if (value.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    "Enter a valid email"
                } else {
                    null
                },
                errorMessage = null,
            )
        }
    }

    fun updatePassword(value: String) {
        _uiState.update { current ->
            current.copy(
                password = value,
                passwordError = if (value.length < 8) "Password must be at least 8 characters" else null,
                errorMessage = null,
            )
        }
    }

    fun clearErrors() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    fun login() {
        val current = _uiState.value
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (!canSubmit(emailError, passwordError)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runAuthOperation {
                repository.login(current.email.trim(), current.password)
            }
        }
    }

    fun register() {
        val current = _uiState.value
        val nameError = if (current.name.isBlank()) "Name is required" else null
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        _uiState.update { it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError) }
        if (!canSubmit(nameError, emailError, passwordError)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runAuthOperation {
                repository.register(
                    name = current.name.trim(),
                    email = current.email.trim(),
                    password = current.password,
                )
            }
        }
    }

    fun handleGoogleIdToken(idToken: String) {
        Log.i(logTag, "Handling Google ID token (len=${idToken.length})")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runAuthOperation {
                Log.i(logTag, "Calling backend Google sign-in")
                repository.googleSignIn(idToken)
            }
        }
    }

    fun handleOAuthRedirect(uri: Uri?) {
        if (uri == null || uri.scheme != "domotics" || uri.host != "auth") return
        val code = uri.getQueryParameter("code") ?: return
        val state = uri.getQueryParameter("state")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runAuthOperation {
                Log.i(logTag, "Exchanging OAuth code via backend")
                repository.exchangeOAuthCode(code, state)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clear()
            _uiState.value = AuthUiState()
        }
    }

    private suspend fun runAuthOperation(block: suspend () -> AuthTokens) {
        runCatching { block() }
            .onSuccess { tokens ->
                persistAndMarkAuthenticated(tokens)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Authentication failed",
                        isAuthenticated = false,
                    )
                }
            }
    }

    private suspend fun persistAndMarkAuthenticated(tokens: AuthTokens) {
        tokenStorage.persist(tokens)
        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                errorMessage = null,
            )
        }
    }

    private fun canSubmit(vararg errors: String?): Boolean = errors.all { it == null }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email is required"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email"
        else -> null
    }

    private fun validatePassword(password: String): String? =
        if (password.length < 8) "Password must be at least 8 characters" else null
}

class AuthViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val tokenStorage = SecureTokenStorage(context.applicationContext)
        val repository = AuthRepository(
            api = AuthApiService.create(
                baseUrl = BuildConfig.API_BASE_URL,
                tokenProvider = tokenStorage,
            )
        )

        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository, tokenStorage) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
