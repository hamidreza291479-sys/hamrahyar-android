package com.lucifer.hamrahyar.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.home.components.SectionHeader
import com.lucifer.hamrahyar.ui.home.components.ServiceCard
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.domain.model.SupportChannel
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import com.lucifer.hamrahyar.ui.theme.utils.SupportIntentHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    repository: OnlineServiceRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val json = remember { Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true } }
    var channels by remember { mutableStateOf<List<SupportChannel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        isLoading = true
        // Load from cache first
        if (retryTrigger == 0) {
            val cachedJson = preferenceManager.supportChannelsJson.first()
            if (cachedJson != null) {
                try {
                    channels = json.decodeFromString<List<SupportChannel>>(cachedJson)
                    isLoading = false
                } catch (e: Exception) { }
            }
        }

        // Then observe from server
        try {
            repository.observeSupportChannels().collectLatest { updated ->
                channels = updated
                isLoading = false
                // Save to cache
                try {
                    preferenceManager.saveSupportChannels(json.encodeToString(updated))
                } catch (e: Exception) { }
            }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63))
                    )
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("پشتیبانی", fontFamily = Lalezar, fontSize = 22.sp, color = Color.White)
            }

            if (isLoading && channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("دریافت اطلاعات پشتیبانی مقدور نیست.", fontFamily = Vazir, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تلاش مجدد", fontFamily = Vazir)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    itemsIndexed(channels) { index, channel ->
                        val config = getChannelConfig(channel.type)
                        ServiceCard(
                            title = config.displayName,
                            icon = config.icon,
                            color = config.color,
                            description = "ارتباط مستقیم در ${config.displayName}",
                            index = index,
                            onClick = {
                                SupportIntentHandler.openSupport(context, channel.type, channel.identifier)
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class ChannelConfig(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
)

private fun getChannelConfig(type: String): ChannelConfig {
    return when (type.lowercase()) {
        "telegram" -> ChannelConfig("تلگرام", Icons.AutoMirrored.Rounded.Send, Color(0xFF2196F3))
        "eitaa" -> ChannelConfig("ایتا", Icons.AutoMirrored.Rounded.Chat, Color(0xFFE67E22))
        "rubika" -> ChannelConfig("روبیکا", Icons.Rounded.ChatBubble, Color(0xFF8E44AD))
        "email" -> ChannelConfig("ایمیل", Icons.Rounded.Email, Color(0xFFE91E63))
        else -> ChannelConfig("پشتیبانی", Icons.Rounded.HelpOutline, Color.Gray)
    }
}
