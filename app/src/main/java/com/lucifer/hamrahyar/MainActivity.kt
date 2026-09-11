package com.lucifer.hamrahyar

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.lucifer.hamrahyar.ui.navigation.AppNavigator
import com.lucifer.hamrahyar.ui.theme.HamrahyarTheme
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import com.lucifer.hamrahyar.ui.theme.data.repository.OnlineServiceRepositoryImpl

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferenceManager = remember { PreferenceManager(this) }
            val repository = remember { OnlineServiceRepositoryImpl(this) }
            val isDarkModePref by preferenceManager.isDarkMode.collectAsState(initial = null)
            
            val darkTheme = isDarkModePref ?: isSystemInDarkTheme()
            
            HamrahyarTheme(darkTheme = darkTheme) {
                AppNavigator(repository = repository)
            }
        }
    }
}
