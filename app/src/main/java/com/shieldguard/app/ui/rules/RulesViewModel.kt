package com.shieldguard.app.ui.rules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldguard.app.data.local.entity.*
import com.shieldguard.app.data.repository.ShieldRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ShieldRepository(application)

    val rules: StateFlow<List<RuleEntity>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowlist: StateFlow<List<AllowlistEntity>> = repository.allAllowlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleRule(rule: RuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun addCustomRule(pattern: String, type: RuleType, description: String) {
        viewModelScope.launch {
            repository.insertRule(
                RuleEntity(
                    pattern = pattern,
                    type = type,
                    action = RuleAction.BLOCK,
                    description = description,
                    isEnabled = true,
                    isUserCreated = true
                )
            )
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            repository.deleteRule(ruleId)
        }
    }

    fun addAllowlistEntry(value: String, type: AllowlistType, note: String) {
        viewModelScope.launch {
            repository.insertAllowlist(
                AllowlistEntity(
                    value = value,
                    type = type,
                    note = note
                )
            )
        }
    }

    fun deleteAllowlistEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteAllowlist(id)
        }
    }
}
