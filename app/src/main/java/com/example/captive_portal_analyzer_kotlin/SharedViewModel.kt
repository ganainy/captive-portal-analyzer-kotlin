package com.example.captive_portal_analyzer_kotlin

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.components.ToastState
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale


enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}


class SharedViewModel(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    /*show action alert dialogs from anywhere in the app*/
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState = _dialogState.asStateFlow()


    private val _clickedSessionId = MutableStateFlow<String?>(null)
    val clickedSessionId: StateFlow<String?> = _clickedSessionId

    private val _clickedWebViewRequestEntity = MutableStateFlow<CustomWebViewRequestEntity?>(null)
    val clickedWebViewRequestEntity: StateFlow<CustomWebViewRequestEntity?> = _clickedWebViewRequestEntity

    private val _toastState = MutableStateFlow<ToastState>(ToastState.Hidden)
    val toastState = _toastState.asStateFlow()


    fun updateClickedSessionId(clickedSessionId: String?) {
        _clickedSessionId.value = clickedSessionId
    }

    //is device connected to the internet
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    //settings related to app language

    private val languageKey = stringPreferencesKey("app_language") // Keys for DataStore
    private val _currentLocale = MutableStateFlow<Locale>(Locale("en"))
    val currentLocale: StateFlow<Locale> get() = _currentLocale


    //settings related to dark/light mode

    private val themeModeKey = stringPreferencesKey("theme_mode")  // Keys for DataStore
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    //the clicked webpage content to show in the WebpageContent screen
    private val _clickedWebpageContent = MutableStateFlow<WebpageContentEntity?>(null)
    val clickedWebpageContent: StateFlow<WebpageContentEntity?> = _clickedWebpageContent.asStateFlow()

    init {
        getLocalePreference()
        getThemePreference()
        viewModelScope.launch {
            connectivityObserver.observe().collect { isConnected ->
                _isConnected.value = isConnected
            }

        }
    }



    private fun getLocalePreference() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.data
                    .map { preferences ->
                        Locale.forLanguageTag(preferences[languageKey] ?: Locale.getDefault().language)
                    }
                    .catch { e ->
                        Log.e("SharedViewModel", "Error reading locale preference", e)
                        emit(Locale.getDefault())
                    }
                    .firstOrNull()?.let {
                        _currentLocale.value = it
                    }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Unexpected error in getLocalePreference", e)
            }
        }
    }

    fun updateLocale(locale: Locale) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[languageKey] = locale.toLanguageTag()
            }
            _currentLocale.value = locale
        }
    }




    private fun getThemePreference() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { preferences ->
                    ThemeMode.valueOf(preferences[themeModeKey] ?: ThemeMode.SYSTEM.name)
                }
                .catch { e ->
                    Log.e("SharedViewModel", "Error reading theme preference", e)
                    emit(ThemeMode.SYSTEM)
                }
                .firstOrNull()?.let {
                    _themeMode.value = it
                }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[themeModeKey] = mode.name
            }
            _themeMode.value = mode
        }
    }


    fun showDialog(
        title: String,
        message: String,
        confirmText: String = "OK",
        dismissText: String = "Cancel",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        _dialogState.value = DialogState.Shown(
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    fun hideDialog() {
        _dialogState.value = DialogState.Hidden
    }


    fun showToast(
        title: String? = null,
        message: String,
        style: ToastStyle,
        duration: Long? = 2000L
    ) {
        _toastState.value = ToastState.Shown(title, message, style, duration)
    }


    fun hideToast() {
        _toastState.value = ToastState.Hidden
    }


    /**
     * Updates the clicked content to show in the WebpageContent screen.
     * @param webpageContentEntity the webpage content entity to be updated.
     */
    fun updateClickedContent(webpageContentEntity: WebpageContentEntity) {
        _clickedWebpageContent.value = webpageContentEntity
    }

    fun updateClickedRequest(webViewRequestEntity: CustomWebViewRequestEntity) {
        _clickedWebViewRequestEntity.value = webViewRequestEntity
    }

}

class SharedViewModelFactory(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val dataStore: DataStore<Preferences>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedViewModel(
                connectivityObserver,
                dataStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


