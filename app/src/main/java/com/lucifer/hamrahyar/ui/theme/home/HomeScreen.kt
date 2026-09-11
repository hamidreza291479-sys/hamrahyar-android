package com.lucifer.hamrahyar.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.lucifer.hamrahyar.MainActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.home.components.AlertOverlay
import com.lucifer.hamrahyar.ui.home.components.ExitDialog
import com.lucifer.hamrahyar.ui.home.components.ServiceCard
import com.lucifer.hamrahyar.ui.home.components.SectionHeader
import com.lucifer.hamrahyar.ui.home.model.ServiceCategory
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.domain.model.ServiceRequest
import com.lucifer.hamrahyar.ui.theme.utils.NetworkManager
import com.lucifer.hamrahyar.ui.theme.utils.PersianDateUtil
import com.lucifer.hamrahyar.ui.theme.utils.TimeManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    activeRequest: ServiceRequest?,
    categories: List<ServiceCategory>,
    isLoading: Boolean,
    onActiveRequestClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onSubServiceClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current as ComponentActivity
    var currentTime by remember { mutableStateOf(TimeManager.getCurrentTimeMillis()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isInternetAvailable by remember { mutableStateOf(true) }
    var isVpnActive by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Double back to exit logic
    var backPressedTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val now = System.currentTimeMillis()
        if (now - backPressedTime < 2000) {
            onExit()
        } else {
            backPressedTime = now
            Toast.makeText(context, "برای خروج دوباره دکمه برگشت را بزنید", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) categories
        else {
            categories.mapNotNull { category ->
                val filteredServices = category.services.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    (it.description?.contains(searchQuery, ignoreCase = true) ?: false)
                }
                if (filteredServices.isNotEmpty() || category.title.contains(searchQuery, ignoreCase = true)) {
                    category.copy(services = filteredServices.ifEmpty { category.services })
                } else null
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isInternetAvailable = NetworkManager.isInternetAvailable(context)
            isVpnActive = NetworkManager.isVpnActive(context)
            delay(2000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = TimeManager.getCurrentTimeMillis()
            delay(1000)
        }
    }

    val timeText = SimpleDateFormat("HH:mm:ss", Locale("fa")).apply { timeZone = TimeManager.getTehranTimeZone() }.format(Date(currentTime))
        .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
    val dateText = PersianDateUtil.getDateText(currentTime)

    val bgColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Adaptive Tech/Neon Background
                // Check if background is dark by its luminance or just comparing to a known dark color
                val isDark = bgColor.value == 0xFF121212.toULong() || bgColor.value == 0xFF000000.toULong() || bgColor.red < 0.2f
                
                val topColor = if (isDark) Color(0xFF0F0C29) else Color(0xFFF0F2F5)
                val midColor = if (isDark) Color(0xFF302B63) else Color(0xFFE8EAF6)
                val bottomColor = if (isDark) Color(0xFF24243E) else Color(0xFFC5CAE9)
                
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(topColor, midColor, bottomColor)
                    )
                )
                // Subtle tech shapes
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.1f), Color.Transparent)
                    ),
                    center = Offset(size.width * 0.9f, size.height * 0.1f),
                    radius = 500f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.9f),
                    radius = 400f
                )
                // Diagonal accent line
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.03f),
                    start = Offset(0f, size.height * 0.3f),
                    end = Offset(size.width, size.height * 0.7f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top Bar ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showExitDialog = true }, 
                    shape = CircleShape, 
                    color = onSurfaceColor.copy(alpha = 0.1f), 
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = onSurfaceColor, modifier = Modifier.size(22.dp)) 
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .background(onSurfaceColor.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dateText, fontFamily = Lalezar, fontSize = 18.sp, color = onSurfaceColor)
                        Text(timeText, fontFamily = Lalezar, fontSize = 16.sp, color = onSurfaceColor.copy(alpha = 0.7f))
                    }
                }

                Surface(
                    onClick = { onSettingsClick() }, 
                    shape = CircleShape, 
                    color = onSurfaceColor.copy(alpha = 0.1f), 
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(Icons.Rounded.Settings, null, tint = onSurfaceColor, modifier = Modifier.size(22.dp)) 
                    }
                }
            }

            // --- Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp),
                placeholder = { 
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        Text(
                            "جستجوی خدمات...", 
                            fontFamily = Vazir, 
                            color = onSurfaceColor.copy(alpha = 0.4f), 
                            fontSize = 14.sp,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        ) 
                    }
                },
                leadingIcon = { 
                    Icon(
                        Icons.Rounded.Search, 
                        null, 
                        tint = onSurfaceColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, null, tint = onSurfaceColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(
                    fontFamily = Vazir,
                    fontSize = 14.sp,
                    color = onSurfaceColor,
                    textAlign = TextAlign.Start
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = onSurfaceColor.copy(alpha = 0.08f),
                    unfocusedContainerColor = onSurfaceColor.copy(alpha = 0.05f),
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.1f),
                    cursorColor = primaryColor
                ),
                singleLine = true
            )

            // --- Active Request ---
            activeRequest?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp).clickable(onClick = onActiveRequestClick),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00CEC9), CircleShape)
                                .drawBehind {
                                    drawCircle(color = Color(0xFF00CEC9).copy(alpha = 0.4f), radius = 12.dp.toPx())
                                }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("شما یک سفارش فعال دارید", fontFamily = Lalezar, fontSize = 14.sp, color = onSurfaceColor)
                            Text(it.serviceName ?: "خدمت انتخابی", fontFamily = Vazir, fontSize = 11.sp, color = onSurfaceColor.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("ادامه گفتگو", fontFamily = Vazir, fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Optimized Vertical Services List ---
            if (isLoading && categories.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryColor) }
            } else if (filteredCategories.isEmpty() && searchQuery.isNotEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Search, null, tint = onSurfaceColor.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("نتیجه‌ای برای جستجوی شما یافت نشد", fontFamily = Vazir, color = onSurfaceColor.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else if (filteredCategories.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("در حال دریافت لیست خدمات...", fontFamily = Vazir, color = onSurfaceColor.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    filteredCategories.forEach { category ->
                        item(key = category.id) {
                            SectionHeader(title = category.title, textColor = onSurfaceColor)
                        }
                        itemsIndexed(category.services, key = { _, s -> s.id }) { index, service ->
                            ServiceCard(
                                title = service.title,
                                icon = service.icon,
                                color = category.color,
                                description = service.description,
                                hasActiveOrder = activeRequest?.subServiceId == service.id,
                                index = index,
                                onClick = { onSubServiceClick(service.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    ExitDialog(visible = showExitDialog, onDismiss = { showExitDialog = false }, onExit = { (context as? ComponentActivity)?.finishAffinity() })
    if (!isInternetAvailable) AlertOverlay("لطفا اتصال اینترنت را چک کنید", true)
    else if (isVpnActive) AlertOverlay("فیلترشکن را خاموش کنید", true)
}
