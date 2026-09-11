package com.lucifer.hamrahyar.ui.theme.service

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.home.components.PlateInput
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormResponseDto
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Composable
fun FormRequestCard(
    request: FormRequestDto,
    response: FormResponseDto? = null,
    onSubmit: (String) -> Unit
) {
    val isSubmitted = response != null || request.status == "submitted"
    val isExpired = request.status == "expired"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubmitted) Color(0xFF00B894).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isSubmitted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B894).copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSubmitted) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = if (isSubmitted) Color(0xFF00B894) else Color(0xFFa29bfe),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = request.prompt,
                    fontFamily = Lalezar,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isSubmitted) {
                SubmittedFormView(response)
            } else if (isExpired) {
                Text("این فرم منقضی شده است.", color = Color.Red.copy(alpha = 0.7f), fontFamily = Vazir, fontSize = 13.sp)
            } else {
                when (request.type) {
                    "numeric" -> NumericForm(onSubmit)
                    "vehicle_plate" -> PlateForm("car", onSubmit)
                    "motorcycle_plate" -> PlateForm("motor", onSubmit)
                    "text" -> TextForm(onSubmit)
                    "timed" -> TimedForm(request, onSubmit)
                    "delivery_method" -> DeliveryMethodForm(onSubmit)
                    "invoice" -> InvoiceFormView(request, onSubmit)
                    "file" -> FileForm(request, onSubmit)
                    "card_to_card_payment" -> CardPaymentForm(request, onSubmit)
                    "survey" -> SurveyForm(request, onSubmit)
                    "end_service_summary" -> ServiceSummaryView(request, onSubmit)
                    else -> {
                        Text("نوع فرم ناشناخته: ${request.type}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SubmittedFormView(response: FormResponseDto?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("پاسخ شما ارسال شده است:", color = Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val responseData = response?.data?.toString() ?: "تکمیل شده"
            Text(
                text = responseData,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontFamily = Vazir,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun NumericForm(onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { c -> c.isDigit() }) value = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("فقط عدد وارد کنید", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Button(
            onClick = { onSubmit(buildJsonObject { put("value", value) }.toString()) },
            modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
            enabled = value.isNotBlank(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ارسال پاسخ", fontFamily = Vazir)
        }
    }
}

@Composable
fun TextForm(onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("متن خود را اینجا بنویسید...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Button(
            onClick = { onSubmit(buildJsonObject { put("value", value) }.toString()) },
            modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
            enabled = value.isNotBlank(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ارسال پاسخ", fontFamily = Vazir)
        }
    }
}

@Composable
fun PlateForm(type: String, onSubmit: (String) -> Unit) {
    var plateValue by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlateInput(
            plateType = if (type == "car") "خودرو" else "موتور",
            initialValue = "",
            label = if (type == "car") "شماره پلاک خودرو" else "شماره پلاک موتور",
            onValueChange = { plateValue = it }
        )
        Button(
            onClick = { onSubmit(buildJsonObject { put("plate", plateValue) }.toString()) },
            modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
            enabled = plateValue.isNotBlank() && plateValue.length > 5,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("تأیید و ارسال پلاک", fontFamily = Vazir)
        }
    }
}

@Composable
fun TimedForm(request: FormRequestDto, onSubmit: (String) -> Unit) {
    var timeLeft by remember { mutableLongStateOf(0L) }
    var expired by remember { mutableStateOf(false) }

    LaunchedEffect(request.expiresAt) {
        val expiry = request.expiresAt?.let { 
            try { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() } catch(e: Exception) { 0L }
        } ?: 0L
        
        while (expiry > System.currentTimeMillis()) {
            timeLeft = (expiry - System.currentTimeMillis()) / 1000
            delay(1.seconds)
        }
        expired = true
    }

    Column {
        if (!expired) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = Color(0xFFFDCB6E), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val min = timeLeft / 60
                val sec = timeLeft % 60
                Text(
                    text = "${String.format(Locale.US, "%02d:%02d", min, sec)} زمان باقی‌مانده",
                    color = Color(0xFFFDCB6E),
                    fontFamily = Vazir,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumericForm(onSubmit)
        } else {
            Text("زمان این فرم به پایان رسیده است.", color = Color.Red.copy(alpha = 0.7f), fontFamily = Vazir, fontSize = 13.sp)
        }
    }
}

@Composable
fun DeliveryMethodForm(onSubmit: (String) -> Unit) {
    val methods = listOf(
        "Telegram" to Icons.AutoMirrored.Rounded.Send,
        "Eitaa" to Icons.AutoMirrored.Rounded.Chat,
        "Rubika" to Icons.Rounded.ChatBubble,
        "Email" to Icons.Rounded.Email,
        "Postal" to Icons.Rounded.LocalPostOffice
    )
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var detailValue by remember { mutableStateOf("") }

    Column {
        methods.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, icon) ->
                    val isSelected = selectedMethod == name
                    Surface(
                        onClick = { selectedMethod = name },
                        modifier = Modifier.weight(1f).height(45.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF6C5CE7) else Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFa29bfe) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(icon, null, tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(name, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
                if (row.size < 3) Spacer(modifier = Modifier.weight(3f - row.size))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedMethod != null) {
            OutlinedTextField(
                value = detailValue,
                onValueChange = { detailValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (selectedMethod == "Postal") "آدرس کامل پستی" else "شناسه کاربری یا ایمیل", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = { onSubmit(buildJsonObject { put("method", selectedMethod!!); put("detail", detailValue) }.toString()) },
                modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
                enabled = detailValue.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("ارسال", fontFamily = Vazir)
            }
        }
    }
}

@Composable
fun InvoiceFormView(request: FormRequestDto, onSubmit: (String) -> Unit) {
    val total = request.schema?.get("total")?.jsonPrimitive?.content ?: "0"
    Column {
        Text("مبلغ فاکتور: $total تومان", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = Vazir)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onSubmit(buildJsonObject { put("accepted", true) }.toString()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("مشاهده و پرداخت", fontFamily = Vazir)
        }
    }
}

@Composable
fun FileForm(request: FormRequestDto, onSubmit: (String) -> Unit) {
    val maxSize = request.schema?.get("max_size_mb")?.jsonPrimitive?.content?.toIntOrNull() ?: 5
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    Column {
        Text("حداکثر حجم مجاز: $maxSize مگابایت", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = Vazir)
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            onClick = { /* Trigger File Picker */ selectedFileName = "doc_scan.pdf" },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudUpload, null, tint = Color(0xFFa29bfe))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    selectedFileName ?: "انتخاب فایل...",
                    color = if (selectedFileName == null) Color.White.copy(alpha = 0.4f) else Color.White,
                    fontSize = 14.sp,
                    fontFamily = Vazir
                )
            }
        }
        
        if (selectedFileName != null) {
            Button(
                onClick = { onSubmit(buildJsonObject { put("file_ref", "storage/orders/file_123") }.toString()) },
                modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("شروع آپلود و ارسال", fontFamily = Vazir)
            }
        }
    }
}

@Composable
fun CardPaymentForm(request: FormRequestDto, onSubmit: (String) -> Unit) {
    var cardLastFour by remember { mutableStateOf("") }
    var trackingCode by remember { mutableStateOf("") }
    
    Column {
        Text("لطفاً مشخصات واریز را وارد کنید:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = Vazir)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = cardLastFour,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) cardLastFour = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("۴ رقم آخر کارت", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = trackingCode,
            onValueChange = { trackingCode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("کد پیگیری یا شماره ارجاع", fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        
        Button(
            onClick = { onSubmit(buildJsonObject { put("last_four", cardLastFour); put("ref", trackingCode) }.toString()) },
            modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
            enabled = cardLastFour.length == 4 && trackingCode.isNotBlank(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ثبت پرداخت", fontFamily = Vazir)
        }
    }
}

@Composable
fun SurveyForm(request: FormRequestDto, onSubmit: (String) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { i ->
                val index = i + 1
                IconButton(onClick = { rating = index }) {
                    Icon(
                        imageVector = if (index <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null,
                        tint = if (index <= rating) Color(0xFFFFD700) else Color.Gray
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("نظر شما (اختیاری)", fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        
        Button(
            onClick = { onSubmit(buildJsonObject { put("rating", rating); put("comment", comment) }.toString()) },
            modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
            enabled = rating > 0,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ثبت نظرسنجی", fontFamily = Vazir)
        }
    }
}

@Composable
fun ServiceSummaryView(request: FormRequestDto, onSubmit: (String) -> Unit) {
    Column {
        Text("خدمت شما با موفقیت به پایان رسید.", color = Color(0xFF00B894), fontWeight = FontWeight.Bold, fontFamily = Vazir)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            request.schema?.get("summary")?.jsonPrimitive?.content ?: "تمامی مراحل با موفقیت انجام شد.",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontFamily = Vazir
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(buildJsonObject { put("confirmed", true) }.toString()) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("متوجه شدم", fontFamily = Vazir)
        }
    }
}
