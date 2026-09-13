package com.lucifer.hamrahyar.ui.theme.data.remote

import android.content.Context
import com.lucifer.hamrahyar.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.concurrent.TimeUnit

/**
 * Central Supabase Client for the Hamrahyar application.
 * This client provides access to Postgrest, Auth, Realtime, and Storage.
 */
object SupabaseClient {
    private var _client: SupabaseClient? = null
    
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("SupabaseClient not initialized. Call init(context) first.")

    fun init(context: Context) {
        if (_client != null) return
        
        _client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
            
            install(Postgrest)
            install(Auth) {
                sessionManager = PreferenceSessionManager(context.applicationContext)
            }
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }
}

/**
 * A custom session manager that persists the Supabase session in SharedPreferences.
 */
class PreferenceSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        val sessionStr = json.encodeToString(session)
        prefs.edit().putString("session", sessionStr).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val sessionStr = prefs.getString("session", null) ?: return null
        return try {
            json.decodeFromString<UserSession>(sessionStr)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("session").apply()
    }
}
