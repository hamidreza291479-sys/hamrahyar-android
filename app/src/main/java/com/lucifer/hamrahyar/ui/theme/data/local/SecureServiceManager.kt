package com.lucifer.hamrahyar.ui.theme.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lucifer.hamrahyar.ui.theme.domain.model.ActiveService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log

class SecureServiceManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_service_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun saveService(service: ActiveService) {
        val services = getAllServices().toMutableList()
        val index = services.indexOfFirst { 
            it.accessToken == service.accessToken || (it.orderId == service.orderId && it.orderId.isNotEmpty()) 
        }
        if (index != -1) {
            services[index] = service
        } else {
            services.add(service)
        }
        persist(services)
    }

    fun getAllServices(): List<ActiveService> {
        val data = sharedPreferences.getString("active_services", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ActiveService>>(data)
        } catch (e: Exception) {
            Log.e("SecureServiceManager", "Failed to decode services", e)
            emptyList()
        }
    }

    fun removeService(accessToken: String) {
        val services = getAllServices().filter { it.accessToken != accessToken }
        persist(services)
    }
    
    fun replaceServices(services: List<ActiveService>) {
        persist(services)
    }

    fun clearAll() {
        sharedPreferences.edit().remove("active_services").apply()
    }

    private fun persist(services: List<ActiveService>) {
        try {
            val data = json.encodeToString(services)
            sharedPreferences.edit().putString("active_services", data).apply()
        } catch (e: Exception) {
            Log.e("SecureServiceManager", "Persist failed", e)
        }
    }
}
