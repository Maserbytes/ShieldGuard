package com.shieldguard.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.shieldguard.app.data.local.AppDatabase
import com.shieldguard.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShieldRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val ruleDao = db.ruleDao()
    private val allowlistDao = db.allowlistDao()
    private val blockLogDao = db.blockLogDao()

    val allRules: Flow<List<RuleEntity>> = ruleDao.getAllRules()
    val allAllowlist: Flow<List<AllowlistEntity>> = allowlistDao.getAllAllowlist()
    val allLogs: Flow<List<BlockLogEntity>> = blockLogDao.getAllLogs()

    fun getLogsByChannel(channel: CommunicationChannel): Flow<List<BlockLogEntity>> =
        blockLogDao.getLogsByChannel(channel)

    val totalBlockedCount: Flow<Int> = blockLogDao.getTotalBlockedCount()
    val callBlockedCount: Flow<Int> = blockLogDao.getBlockedCountByChannel(CommunicationChannel.CALL)
    val smsBlockedCount: Flow<Int> = blockLogDao.getBlockedCountByChannel(CommunicationChannel.SMS)
    val whatsappBlockedCount: Flow<Int> = blockLogDao.getBlockedCountByChannel(CommunicationChannel.WHATSAPP)
    val gmailBlockedCount: Flow<Int> = blockLogDao.getBlockedCountByChannel(CommunicationChannel.GMAIL)

    suspend fun getActiveRules(): List<RuleEntity> = withContext(Dispatchers.IO) {
        ruleDao.getActiveRules()
    }

    suspend fun getAllowlist(): List<AllowlistEntity> = withContext(Dispatchers.IO) {
        allowlistDao.getActiveAllowlist()
    }

    suspend fun insertRule(rule: RuleEntity) = withContext(Dispatchers.IO) {
        ruleDao.insertRule(rule)
    }

    suspend fun updateRule(rule: RuleEntity) = withContext(Dispatchers.IO) {
        ruleDao.updateRule(rule)
    }

    suspend fun deleteRule(ruleId: Long) = withContext(Dispatchers.IO) {
        ruleDao.deleteRuleById(ruleId)
    }

    suspend fun insertAllowlist(entry: AllowlistEntity) = withContext(Dispatchers.IO) {
        allowlistDao.insert(entry)
    }

    suspend fun deleteAllowlist(id: Long) = withContext(Dispatchers.IO) {
        allowlistDao.deleteById(id)
    }

    suspend fun logBlockedEvent(
        channel: CommunicationChannel,
        sender: String,
        snippet: String,
        reason: String
    ) = withContext(Dispatchers.IO) {
        blockLogDao.insertLog(
            BlockLogEntity(
                channel = channel,
                senderOrCaller = sender,
                contentSnippet = snippet,
                reason = reason
            )
        )
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        blockLogDao.clearAllLogs()
    }

    suspend fun deleteLogById(id: Long) = withContext(Dispatchers.IO) {
        blockLogDao.deleteLogById(id)
    }

    /**
     * Checks if a phone number exists in the device's native Contacts.
     * Zero-latency local ContentResolver check.
     */
    suspend fun isContact(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext false
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                return@withContext cursor.count > 0
            }
        } catch (e: SecurityException) {
            // Permission not granted yet
            false
        } catch (e: Exception) {
            false
        }
        false
    }

    suspend fun isExplicitlyAllowed(value: String, type: AllowlistType): Boolean = withContext(Dispatchers.IO) {
        allowlistDao.isAllowed(value, type)
    }

    /**
     * Checks whether a *display name* belongs to a saved contact.
     *
     * Messaging notifications carry a sender name ("Aarav Sharma"), never a phone number, so
     * [isContact] cannot protect them. Without this check a saved friend writing "I'll order
     * now" would be treated as an unknown sender and could be filtered.
     */
    suspend fun isContactByName(displayName: String): Boolean = withContext(Dispatchers.IO) {
        val name = displayName.trim()
        // Single/short names produce far too many coincidental matches to be a safe signal.
        if (name.length < 3) return@withContext false
        try {
            val projection = arrayOf(ContactsContract.Contacts._ID)
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ? COLLATE NOCASE"
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                arrayOf(name),
                null
            )?.use { cursor ->
                return@withContext cursor.moveToFirst()
            }
        } catch (e: SecurityException) {
            // Contacts permission not granted: fail open (never block because of a missing grant)
        } catch (e: Exception) {
            // Same reasoning: an unreadable contacts provider must not turn into silent blocking.
        }
        false
    }
}
