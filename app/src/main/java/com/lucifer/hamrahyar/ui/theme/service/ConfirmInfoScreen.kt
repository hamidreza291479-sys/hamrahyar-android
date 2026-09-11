package com.lucifer.hamrahyar.ui.theme.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.domain.model.PriorityLevel
import com.lucifer.hamrahyar.ui.theme.utils.FormLabelMapper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmInfoScreen(
    serviceName: String,
    userName: String,
    userMobile: String,
    userEmail: String?,
    formData: String?,
    priority: PriorityLevel,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("تأیید نهایی اطلاعات", fontFamily = Lalezar, fontSize = 22.sp, color = Color.White)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoSection(title = "اطلاعات خدمت", icon = Icons.Rounded.DesignServices) {
                    InfoRow(label = "خدمت انتخاب شده", value = serviceName)
                    InfoRow(label = "اولویت بررسی", value = priority.label, valueColor = priority.color)
                }

                InfoSection(title = "اطلاعات فردی", icon = Icons.Rounded.Person) {
                    InfoRow(label = "نام و نام خانوادگی", value = userName)
                    InfoRow(label = "شماره همراه", value = userMobile)
                    if (!userEmail.isNullOrBlank()) {
                        InfoRow(label = "ایمیل", value = userEmail)
                    }
                }

                if (!formData.isNullOrBlank()) {
                    val fieldList = remember(formData) {
                        try {
                            val jsonElement = Json.parseToJsonElement(formData!!).jsonObject
                            jsonElement.mapNotNull { (key, value) ->
                                if (key != "mobile" && key != "full_name" && key != "email" && key != "guest_key") {
                                    val rawValue = value.jsonPrimitive.content
                                    val displayValue = when (rawValue) {
                                        "true" -> "بله"
                                        "false" -> "خیر"
                                        else -> rawValue
                                    }
                                    key to displayValue
                                } else null
                            }
                        } catch (e: Exception) { emptyList<Pair<String, String>>() }
                    }

                    if (fieldList.isNotEmpty()) {
                        InfoSection(title = "جزئیات درخواست", icon = Icons.Rounded.Description) {
                            fieldList.forEach { pair ->
                                InfoRow(label = FormLabelMapper.map(pair.first), value = pair.second)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأیید و ثبت نهایی", fontFamily = Lalezar, fontSize = 18.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Edit, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ویرایش اطلاعات", fontFamily = Lalezar, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun InfoSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFFa29bfe), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontFamily = Lalezar, fontSize = 18.sp, color = Color(0xFFa29bfe))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = Vazir, fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        Text(value, fontFamily = Vazir, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
