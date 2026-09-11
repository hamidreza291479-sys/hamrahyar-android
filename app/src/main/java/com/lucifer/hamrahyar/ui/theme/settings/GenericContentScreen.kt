package com.lucifer.hamrahyar.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.domain.model.AppContent
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericContentScreen(
    contentKey: String,
    repository: OnlineServiceRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val json = remember { Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true } }
    var content by remember { mutableStateOf<AppContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(contentKey, retryTrigger) {
        isLoading = true
        errorMessage = null
        
        // Load from cache first (only on initial load)
        if (retryTrigger == 0) {
            val cacheFlow = if (contentKey == "about_content") preferenceManager.aboutContent else preferenceManager.termsContent
            val cachedJson = cacheFlow.first()
            if (cachedJson != null) {
                try {
                    content = json.decodeFromString<AppContent>(cachedJson)
                    isLoading = false
                } catch (e: Exception) { }
            }
        }

        // Fetch/Observe from server
        try {
            repository.observeAppContent(contentKey).collectLatest { updated ->
                content = updated
                isLoading = false
                errorMessage = null
                // Save to cache
                try {
                    val jsonStr = json.encodeToString(updated)
                    if (contentKey == "about_content") {
                        preferenceManager.saveAboutContent(jsonStr)
                    } else {
                        preferenceManager.saveTermsContent(jsonStr)
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) {
            if (content == null) {
                errorMessage = "دریافت اطلاعات انجام نشد. لطفا اتصال اینترنت خود را بررسی کنید."
            }
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
                Text(
                    content?.title ?: if (contentKey == "about_content") "درباره همراه‌یار" else "قوانین و مقررات", 
                    fontFamily = Lalezar, fontSize = 22.sp, color = Color.White
                )
            }

            if (isLoading && content == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (errorMessage != null && content == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(errorMessage!!, fontFamily = Vazir, color = Color.White.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content?.body ?: "",
                        fontFamily = Vazir,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 28.sp,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}
