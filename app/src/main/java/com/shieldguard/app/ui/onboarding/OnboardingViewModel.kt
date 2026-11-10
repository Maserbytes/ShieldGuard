package com.shieldguard.app.ui.onboarding

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingState(
    val currentStep: Int = 1,
    val isPhoneRoleGranted: Boolean = false,
    val isSmsRoleGranted: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isBatteryOptimizedExempted: Boolean = false,
    val isSetupComplete: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        checkCurrentPermissions()
    }

    fun checkCurrentPermissions() {
        val context = getApplication<Application>()
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else null

        val phoneGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER) || roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else true

        val smsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else true

        val notificationGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        _state.value = _state.value.copy(
            isPhoneRoleGranted = phoneGranted,
            isSmsRoleGranted = smsGranted,
            isNotificationAccessGranted = notificationGranted,
            isBatteryOptimizedExempted = batteryExempt
        )
    }

    fun nextStep() {
        if (_state.value.currentStep < 5) {
            _state.value = _state.value.copy(currentStep = _state.value.currentStep + 1)
        } else {
            _state.value = _state.value.copy(isSetupComplete = true)
        }
    }

    fun previousStep() {
        if (_state.value.currentStep > 1) {
            _state.value = _state.value.copy(currentStep = _state.value.currentStep - 1)
        }
    }
}
