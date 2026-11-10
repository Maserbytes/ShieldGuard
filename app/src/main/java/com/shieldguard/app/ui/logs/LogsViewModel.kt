package com.shieldguard.app.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldguard.app.data.local.entity.BlockLogEntity
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.data.repository.ShieldRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ShieldRepository(application)

    private val _selectedFilter = MutableStateFlow<CommunicationChannel?>(null)
    val selectedFilter: StateFlow<CommunicationChannel?> = _selectedFilter.asStateFlow()

    val logs: StateFlow<List<BlockLogEntity>> = _selectedFilter
        .flatMapLatest { channel ->
            if (channel == null) {
                repository.allLogs
            } else {
                repository.getLogsByChannel(channel)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(channel: CommunicationChannel?) {
        _selectedFilter.value = channel
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }
}
