package com.lucifer.hamrahyar.ui.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.home.components.SectionHeader
import com.lucifer.hamrahyar.ui.home.components.ServiceCard
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import kotlinx.coroutines.launch

data class SettingsItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSupportClick: () -> Unit,
    onSecurityClick: (Boolean) -> Unit, // Boolean: true if it's a change/setup request
    onNotificationsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onTermsClick: () -> Unit,
    isPinSet: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // Check if system security is active
    val isSystemSecurityActive = remember {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                           androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        biometricManager.canAuthenticate(authenticators) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    val items = remember(isPinSet, isSystemSecurityActive) {
        listOf(
            SettingsItem("1", "حالت نمایش", Icons.Default.Palette, Color(0xFF9C27B0), "تغییر تم برنامه (تاریک/روشن)"),
            SettingsItem("notifications", "اطلاعیه‌ها", Icons.Default.Notifications, Color(0xFFFF9800), "مشاهده اطلاعیه‌ها و پیام‌های سیستم"),
            SettingsItem(
                "3", 
                if (!isPinSet) "تعیین رمز عبور ۶ رقمی" else "تغییر رمز عبور ۶ رقمی",
                Icons.Default.Lock, 
                if (isSystemSecurityActive) Color.Gray else Color(0xFF3F51B5), 
                if (isSystemSecurityActive) "به دلیل فعال بودن امنیت سیستم، غیرفعال است" else "تأمین امنیت اختصاصی اپلیکیشن"
            ),
            SettingsItem("4", "پشتیبانی", Icons.Default.SupportAgent, Color(0xFF4CAF50), "ارتباط با کارشناسان و واحد پشتیبانی"),
            SettingsItem("about", "درباره همراه‌یار", Icons.Default.Info, Color(0xFF607D8B), "نسخه برنامه و اطلاعات سازنده"),
            SettingsItem("terms", "قوانین و مقررات", Icons.Default.PrivacyTip, Color(0xFFE91E63), "مشاهده حریم خصوصی و شرایط استفاده")
        )
    }

    val bgColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val isDark = bgColor.value == 0xFF121212.toULong() || bgColor.value == 0xFF000000.toULong() || bgColor.red < 0.2f
                
                val topColor = if (isDark) Color(0xFF0F0C29) else Color(0xFFF0F2F5)
                val midColor = if (isDark) Color(0xFF302B63) else Color(0xFFE8EAF6)
                val bottomColor = if (isDark) Color(0xFF24243E) else Color(0xFFC5CAE9)

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(topColor, midColor, bottomColor)
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(onSurfaceColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = null,
                        tint = onSurfaceColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text("تنظیمات", fontFamily = Lalezar, fontSize = 22.sp, color = onSurfaceColor)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    ServiceCard(
                        title = item.title,
                        icon = item.icon,
                        color = item.color,
                        description = item.description,
                        index = index,
                        onClick = {
                            when (item.id) {
                                "1" -> showThemeDialog = true
                                "notifications" -> onNotificationsClick()
                                "3" -> {
                                    if (!isSystemSecurityActive) {
                                        onSecurityClick(true)
                                    } else {
                                        Toast.makeText(context, "امنیت سیستم فعال است و نیازی به رمز عبور نیست", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "4" -> onSupportClick()
                                "about" -> onAboutClick()
                                "terms" -> onTermsClick()
                                "5" -> {
                                    Toast.makeText(context, "نسخه ۱.۰.۰ همراه‌یار", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("انتخاب حالت نمایش", fontFamily = Vazir) },
            text = {
                Column {
                    ThemeOption("تم روشن", Icons.Default.LightMode) {
                        scope.launch {
                            preferenceManager.saveDarkMode(false)
                            showThemeDialog = false
                        }
                    }
                    ThemeOption("تم تاریک", Icons.Default.DarkMode) {
                        scope.launch {
                            preferenceManager.saveDarkMode(true)
                            showThemeDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("انصراف", fontFamily = Vazir)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontFamily = Vazir, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
