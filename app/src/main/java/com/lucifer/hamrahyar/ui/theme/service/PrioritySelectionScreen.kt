package com.lucifer.hamrahyar.ui.theme.service

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.domain.model.PriorityConfig
import com.lucifer.hamrahyar.ui.theme.domain.model.PriorityLevel
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySelectionScreen(
    repository: OnlineServiceRepository,
    onPrioritySelected: (PriorityLevel) -> Unit,
    onBack: () -> Unit
) {
    var configs by remember { mutableStateOf<List<PriorityConfig>>(emptyList()) }
    var selectedLevel by remember { mutableStateOf<PriorityLevel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        configs = repository.getPriorityConfiguration()
        isLoading = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("انتخاب اولویت درخواست", fontFamily = Lalezar) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "لطفاً اولویت انجام خدمت خود را انتخاب کنید:",
                        fontFamily = Vazir,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(configs) { index, config ->
                            PriorityItem(
                                config = config,
                                isSelected = selectedLevel == config.level,
                                index = index,
                                onClick = { selectedLevel = config.level }
                            )
                        }
                    }

                    Button(
                        onClick = { selectedLevel?.let { onPrioritySelected(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .animateContentSize(),
                        enabled = selectedLevel != null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("تایید و ادامه", fontFamily = Lalezar, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityItem(
    config: PriorityConfig,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible.value = true }

    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(animationSpec = tween(500, delayMillis = index * 100)) + 
                slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(500, delayMillis = index * 100))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) config.level.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) config.level.color else Color.LightGray.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = config.level.color)
                )

                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = config.level.label,
                        fontFamily = Lalezar,
                        fontSize = 18.sp,
                        color = config.level.color
                    )
                    Text(
                        text = config.description,
                        fontFamily = Vazir,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "زمان تخمینی: ${config.estimatedTime}",
                        fontFamily = Vazir,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (config.additionalCost > 0) {
                    Text(
                        text = "+ ${config.additionalCost} تومان",
                        fontFamily = Lalezar,
                        fontSize = 14.sp,
                        color = config.level.color
                    )
                } else {
                    Text(
                        text = "رایگان",
                        fontFamily = Lalezar,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
