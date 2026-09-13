package com.lucifer.hamrahyar.ui.theme.data.remote

import android.content.Context
import android.util.Log
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
    private const val TAG = "SupabaseClient"
    private var _client: SupabaseClient? = null
    
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("SupabaseClient not initialized. Call init(context) first.")

    fun init(context: Context) {
        if (_client != null) return
        
        Log.d(TAG, "Initializing SupabaseClient...")
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
    private val TAG = "SessionManager"
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun saveSession(session: UserSession) {
        try {
            val sessionStr = json.encodeToString(session)
            prefs.edit().putString("session", sessionStr).apply()
            Log.d(TAG, "Session saved successfully for user: ${session.user?.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session", e)
        }
    }

    override suspend fun loadSession(): UserSession? {
        val sessionStr = prefs.getString("session", null)
        if (sessionStr == null) {
            Log.d(TAG, "No session found in storage")
            return null
        }
        return try {
            val session = json.decodeFromString<UserSession>(sessionStr)
            Log.d(TAG, "Session loaded successfully for user: ${session.user?.id}")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session from storage", e)
            null
        }
    }

    override suspend fun deleteSession() {
        Log.d(TAG, "Deleting session from storage")
        prefs.edit().remove("session").apply()
    }
}
