package com.lucifer.hamrahyar.ui.theme.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hamrahyar_prefs")

class PreferenceManager(private val context: Context) {

    companion object {
        val CUSTOMER_MOBILE = stringPreferencesKey("customer_mobile")
        val CUSTOMER_NAME = stringPreferencesKey("customer_name")
        val CUSTOMER_EMAIL = stringPreferencesKey("customer_email")
        val ACTIVE_SERVICE_ID = stringPreferencesKey("active_service_id")
        val SERVICE_ACCESS_TOKEN = stringPreferencesKey("service_access_token")
        val ACTIVE_ACCESS_ID = stringPreferencesKey("active_access_id")
        val ACTIVE_CONVERSATION_ID = stringPreferencesKey("active_conversation_id")
        val ACTIVE_SERVICE_JSON = stringPreferencesKey("active_service_json")
        val ACTIVE_CHAT_ID = stringPreferencesKey("active_chat_id")
        val LAST_SERVICE_STATUS = stringPreferencesKey("last_service_status")
        val CHAT_DRAFT = stringPreferencesKey("chat_draft")
        val LAST_ACTIVE_TIME = longPreferencesKey("last_active_time")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val GUEST_KEY = stringPreferencesKey("guest_key")
        val ABOUT_CONTENT_JSON = stringPreferencesKey("about_content_json")
        val TERMS_CONTENT_JSON = stringPreferencesKey("terms_content_json")
        val ANNOUNCEMENTS_JSON = stringPreferencesKey("announcements_json")
        val SUPPORT_CHANNELS_JSON = stringPreferencesKey("support_channels_json")
    }

    val guestKey: Flow<String?> = context.dataStore.data.map { it[GUEST_KEY] }
    val customerMobile: Flow<String?> = context.dataStore.data.map { it[CUSTOMER_MOBILE] }
    val customerName: Flow<String?> = context.dataStore.data.map { it[CUSTOMER_NAME] }
    val customerEmail: Flow<String?> = context.dataStore.data.map { it[CUSTOMER_EMAIL] }
    val activeServiceId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_SERVICE_ID] }
    val serviceAccessToken: Flow<String?> = context.dataStore.data.map { it[SERVICE_ACCESS_TOKEN] }
    val chatDraft: Flow<String?> = context.dataStore.data.map { it[CHAT_DRAFT] }
    val lastActiveTime: Flow<Long?> = context.dataStore.data.map { it[LAST_ACTIVE_TIME] }
    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { it[IS_DARK_MODE] }
    val aboutContent: Flow<String?> = context.dataStore.data.map { it[ABOUT_CONTENT_JSON] }
    val termsContent: Flow<String?> = context.dataStore.data.map { it[TERMS_CONTENT_JSON] }
    val announcementsJson: Flow<String?> = context.dataStore.data.map { it[ANNOUNCEMENTS_JSON] }
    val supportChannelsJson: Flow<String?> = context.dataStore.data.map { it[SUPPORT_CHANNELS_JSON] }

    val activeServiceJson: Flow<String?> = context.dataStore.data.map { it[ACTIVE_SERVICE_JSON] }
    val activeAccessId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_ACCESS_ID] }
    val activeConversationId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_CONVERSATION_ID] }

    suspend fun saveMobile(mobile: String) {
        context.dataStore.edit { it[CUSTOMER_MOBILE] = mobile }
    }

    suspend fun saveDarkMode(isDark: Boolean) {
        context.dataStore.edit { it[IS_DARK_MODE] = isDark }
    }

    suspend fun saveAboutContent(json: String) {
        context.dataStore.edit { it[ABOUT_CONTENT_JSON] = json }
    }

    suspend fun saveTermsContent(json: String) {
        context.dataStore.edit { it[TERMS_CONTENT_JSON] = json }
    }

    suspend fun saveAnnouncements(json: String) {
        context.dataStore.edit { it[ANNOUNCEMENTS_JSON] = json }
    }

    suspend fun saveSupportChannels(json: String) {
        context.dataStore.edit { it[SUPPORT_CHANNELS_JSON] = json }
    }

    suspend fun saveName(name: String) {
        context.dataStore.edit { it[CUSTOMER_NAME] = name }
    }

    suspend fun saveEmail(email: String) {
        context.dataStore.edit { it[CUSTOMER_EMAIL] = email }
    }

    suspend fun saveActiveService(serviceId: String, accessToken: String, accessId: String? = null, conversationId: String? = null) {
        context.dataStore.edit { 
            it[ACTIVE_SERVICE_ID] = serviceId 
            it[SERVICE_ACCESS_TOKEN] = accessToken
            if (accessId != null) it[ACTIVE_ACCESS_ID] = accessId
            if (conversationId != null) it[ACTIVE_CONVERSATION_ID] = conversationId
        }
    }

    suspend fun saveFullActiveService(serviceJson: String) {
        context.dataStore.edit { it[ACTIVE_SERVICE_JSON] = serviceJson }
    }

    suspend fun saveChatDraft(draft: String) {
        context.dataStore.edit { it[CHAT_DRAFT] = draft }
    }

    suspend fun saveLastActiveTime(time: Long) {
        context.dataStore.edit { it[LAST_ACTIVE_TIME] = time }
    }

    suspend fun saveGuestKey(key: String) {
        context.dataStore.edit { it[GUEST_KEY] = key }
    }

    suspend fun clearActiveService() {
        context.dataStore.edit { 
            it.remove(ACTIVE_SERVICE_ID)
            it.remove(SERVICE_ACCESS_TOKEN)
            it.remove(ACTIVE_ACCESS_ID)
            it.remove(ACTIVE_CONVERSATION_ID)
            it.remove(ACTIVE_SERVICE_JSON)
        }
    }
}
