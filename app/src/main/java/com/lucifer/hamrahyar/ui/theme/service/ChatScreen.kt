package com.lucifer.hamrahyar.ui.theme.service

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormResponseDto
import com.lucifer.hamrahyar.ui.theme.domain.model.*
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    request: ServiceRequest,
    repository: OnlineServiceRepository,
    categories: List<com.lucifer.hamrahyar.ui.home.model.ServiceCategory>,
    onActiveRequestUpdate: (ServiceRequest?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var realtimeStatus by remember { mutableStateOf("CONNECTED") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showOrderDetails by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var formRequests by remember { mutableStateOf<List<FormRequestDto>>(emptyList()) }
    var formResponses by remember { mutableStateOf<List<FormResponseDto>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        repository.observeRealtimeStatus().collectLatest { realtimeStatus = it }
    }
    
    val isClosed = request.status == ServiceStatus.CLOSED || request.status == ServiceStatus.ARCHIVED || request.status == ServiceStatus.CANCELLED || request.status == ServiceStatus.REJECTED
    val isSubmitted = request.status == ServiceStatus.SUBMITTED
    var conversationId by remember { mutableStateOf(request.conversationId ?: "") }

    LaunchedEffect(request.conversationId) {
        request.conversationId?.let { conversationId = it }
    }

    LaunchedEffect(conversationId) {
        if (conversationId.isNotEmpty()) {
            launch { repository.observeFormRequests(conversationId).collectLatest { formRequests = it } }
            launch { repository.observeFormResponses(conversationId).collectLatest { formResponses = it } }
        }
    }

    LaunchedEffect(conversationId, request.profileId) {
        if (conversationId.isNotEmpty() && request.profileId != null) {
            repository.getMessages(conversationId, request.profileId).collectLatest { messages = it }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0C29), Color(0xFF1B1B2F), Color(0xFF16213E))
                    )
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White.copy(alpha = 0.03f),
                            titleContentColor = Color.White
                        ),
                        title = {
                            Text(
                                "جزئیات درخواست",
                                fontFamily = Lalezar,
                                fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                        },
                        actions = {
                            if (!isClosed) {
                                IconButton(onClick = { showCancelDialog = true }) {
                                    Icon(Icons.Default.Cancel, null, tint = Color.Red.copy(alpha = 0.6f))
                                }
                            }
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = realtimeStatus != "CONNECTED",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = Color(0xFFFF7675).copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WifiOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اتصال موقتاً قطع شد", color = Color.White, fontFamily = Vazir, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (showCancelDialog) {
                CancelOrderDialog(
                    onDismiss = { showCancelDialog = false },
                    onConfirm = {
                        scope.launch {
                            repository.cancelService(request.id)
                                .onSuccess {
                                    repository.clearLocalAccess()
                                    showCancelDialog = false
                                    Toast.makeText(context, "درخواست با موفقیت لغو شد", Toast.LENGTH_SHORT).show()
                                    onActiveRequestUpdate(null)
                                    onBack()
                                }
                                .onFailure {
                                    Toast.makeText(context, "خطا در لغو درخواست", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    OrderDetailsSummaryCard(request = request)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "فرم‌های مورد نیاز",
                        fontFamily = Lalezar,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                if (formRequests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSubmitted) {
                                Text(
                                    "کارشناسان فرم‌های مربوط را برای شما ارسال می‌کنند.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontFamily = Vazir,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                CircularProgressIndicator(color = Color(0xFFa29bfe))
                            }
                        }
                    }
                }

                items(formRequests, key = { "form_${it.id}" }) { form ->
                    val response = formResponses.find { it.requestId == form.id }
                    FormRequestCard(
                        request = form, 
                        response = response,
                        repository = repository,
                        orderId = request.id,
                        onSubmit = { data ->
                            scope.launch {
                                repository.submitFormResponse(form.id, request.id, data)
                                    .onSuccess {
                                        Toast.makeText(context, "فرم با موفقیت ارسال شد", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, "خطا در ارسال فرم", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    )
                }
                
                val notices = messages.filter { it.senderRole == "ADMIN" && it.type == "text" }
                if (notices.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "اطلاعیه‌های کارشناس",
                            fontFamily = Lalezar,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    items(notices, key = { "msg_${it.id}" }) { notice ->
                        NoticeCard(notice)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            
            if (request.status == ServiceStatus.REJECTED && request.result == "expired_without_acceptance") {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.clearLocalAccess()
                                onActiveRequestUpdate(null)
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("فهمیدم", fontFamily = Lalezar, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Info, null, tint = Color(0xFFFDCB6E), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message.content,
                color = Color.White,
                fontFamily = Vazir,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StatusBadge(status: ServiceStatus) {
    val color = when (status) {
        ServiceStatus.SUBMITTED -> Color(0xFF00CEC9)
        ServiceStatus.UNDER_REVIEW -> Color(0xFF0984E3)
        ServiceStatus.PROCESSING -> Color(0xFF6C5CE7)
        ServiceStatus.READY_DELIVERY -> Color(0xFFE84393)
        ServiceStatus.WAITING_CONFIRMATION, ServiceStatus.WAITING_DOCUMENTS -> Color(0xFFFDCB6E)
        ServiceStatus.WAITING_PAYMENT, ServiceStatus.INVOICE_ISSUED -> Color(0xFFE17055)
        ServiceStatus.PAID -> Color(0xFF00B894)
        ServiceStatus.CLOSED -> Color(0xFF00B894)
        ServiceStatus.REJECTED, ServiceStatus.CANCELLED -> Color(0xFFD63031)
        else -> Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.label,
                fontFamily = Vazir,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderDetailsSummaryCard(request: ServiceRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFa29bfe), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("خلاصه درخواست", fontFamily = Lalezar, fontSize = 16.sp, color = Color.White)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow("نام خدمت", request.serviceName ?: "نامشخص")
                SummaryRow("نام ثبت‌کننده", request.customerFullName ?: "نامشخص")
                SummaryRow("شماره پیگیری", request.trackingNumber)
                SummaryRow("وضعیت فعلی", request.status.label)
                
                val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.forLanguageTag("fa")).format(Date(request.createdAt))
                    .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
                SummaryRow("زمان ثبت", date)
                
                SummaryRow("اولویت", request.priority.label)

                if (request.status == ServiceStatus.INVOICE_ISSUED || request.status == ServiceStatus.WAITING_PAYMENT || request.status == ServiceStatus.PAID || request.status == ServiceStatus.PROCESSING || request.status == ServiceStatus.READY_DELIVERY || request.status == ServiceStatus.CLOSED) {
                    if (request.totalAmount > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.1f))
                        Text("جزئیات مالی (صورتحساب)", fontFamily = Lalezar, fontSize = 14.sp, color = Color(0xFF00CEC9))
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryRow("هزینه خدمت", "${request.serviceAmount.toLong()} تومان")
                        if (request.speedAmount > 0) {
                            SummaryRow("هزینه اولویت (${request.priority.label})", "${request.speedAmount.toLong()} تومان")
                        }
                        if (request.discountAmount > 0) {
                            SummaryRow("تخفیف", "${request.discountAmount.toLong()} تومان")
                        }
                        SummaryRow("مبلغ قابل پرداخت", "${request.totalAmount.toLong()} تومان", isHighlighted = true)
                    }
                }

                if (request.status != ServiceStatus.SUBMITTED && request.adminName != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF6C5CE7).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(request.adminName.take(1), color = Color(0xFFa29bfe), fontFamily = Lalezar, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("پذیرش توسط:", color = Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 10.sp)
                            Text(request.adminName, color = Color.White, fontFamily = Vazir, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (!request.adminRole.isNullOrBlank()) {
                                val translatedRole = when(request.adminRole.lowercase()) {
                                    "admin" -> "مدیر سیستم"
                                    "operator" -> "کارشناس فنی"
                                    "support" -> "پشتیبان"
                                    "expert" -> "کارشناس ارشد"
                                    else -> request.adminRole
                                }
                                Text(translatedRole, color = Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isHighlighted: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 12.sp)
        Text(
            value, 
            color = if (isHighlighted) Color(0xFF00CEC9) else Color.White, 
            fontFamily = Vazir, 
            fontSize = if (isHighlighted) 14.sp else 12.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CancelOrderDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1B2F),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f),
        title = { Text("لغو درخواست", fontFamily = Lalezar) },
        text = { Text("آیا از لغو این درخواست اطمینان دارید؟ این عمل قابل بازگشت نیست.", fontFamily = Vazir) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("لغو درخواست", color = Color(0xFFFF7675), fontFamily = Vazir)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = Color.White, fontFamily = Vazir)
            }
        }
    )
}
