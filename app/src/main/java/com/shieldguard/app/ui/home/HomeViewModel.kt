package com.shieldguard.app.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldguard.app.data.local.entity.BlockLogEntity
import com.shieldguard.app.data.repository.ShieldRepository
import com.shieldguard.app.service.NotificationWatchdog
import com.shieldguard.app.service.ShieldForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ShieldRepository(application)

    val totalBlocked = repository.totalBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val callBlocked = repository.callBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val smsBlocked = repository.smsBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val whatsappBlocked = repository.whatsappBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val gmailBlocked = repository.gmailBlockedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentLogs: StateFlow<List<BlockLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isShieldActive = MutableStateFlow(true)
    val isShieldActive: StateFlow<Boolean> = _isShieldActive

    fun toggleShield(enabled: Boolean) {
        _isShieldActive.value = enabled
        val context = getApplication<Application>()
        if (enabled) {
            ShieldForegroundService.start(context)
            NotificationWatchdog.ensureServiceAlive(context)
        } else {
            ShieldForegroundService.stop(context)
        }
    }
}
