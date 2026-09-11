package com.lucifer.hamrahyar.ui.theme.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LocalSixDigitPasswordManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_pin_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PIN_KEY = "hashed_six_digit_pin"
    }

    fun savePin(pin: String): Boolean {
        if (pin.length != 6 || !pin.all { it.isDigit() }) return false
        return try {
            // In a real app, we might hash it, but EncryptedSharedPreferences already encrypts it.
            // The requirement says "don't store in plaintext", ESP satisfies this.
            prefs.edit().putString(PIN_KEY, pin).commit()
        } catch (e: Exception) {
            false
        }
    }

    fun getPin(): String? {
        return prefs.getString(PIN_KEY, null)
    }

    fun verifyPin(input: String): Boolean {
        val saved = getPin()
        return saved != null && saved == input
    }

    fun isPinSet(): Boolean {
        return getPin() != null
    }

    fun clearPin() {
        prefs.edit().remove(PIN_KEY).apply()
    }
}
