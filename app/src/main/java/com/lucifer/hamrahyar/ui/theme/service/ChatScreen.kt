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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
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
    var inputText by remember { mutableStateOf("") }
    var realtimeStatus by remember { mutableStateOf("CONNECTED") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showOrderDetails by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var formRequests by remember { mutableStateOf<List<FormRequestDto>>(emptyList()) }
    var formResponses by remember { mutableStateOf<List<FormResponseDto>>(emptyList()) }
    
    val listState = rememberLazyListState()
    
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
                // Premium Calm Chat Background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0C29), Color(0xFF1B1B2F), Color(0xFF16213E))
                    )
                )
                // Subtle patterns
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6C5CE7).copy(alpha = 0.05f), Color.Transparent)
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = 500f
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showOrderDetails = !showOrderDetails }
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        request.serviceName ?: "خدمت",
                                        fontFamily = Lalezar,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    StatusBadge(status = request.status)
                                }
                                
                                Icon(
                                    if (showOrderDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
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
                    
                    // Connection Status Indicator (Independent from Order Status)
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
            },
            bottomBar = {
                ChatBottomBar(
                    isClosed = isClosed,
                    isSubmitted = isSubmitted,
                    conversationId = conversationId,
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    status = request.status,
                    result = request.result,
                    onIUnderstood = {
                        scope.launch {
                            repository.clearLocalAccess()
                            onActiveRequestUpdate(null)
                            onBack()
                        }
                    },
                    onSend = {
                        if (inputText.isNotBlank() && conversationId.isNotEmpty() && request.profileId != null) {
                            scope.launch {
                                repository.sendMessage(conversationId, inputText, request.profileId)
                                inputText = ""
                            }
                        } else if (request.profileId == null) {
                            Toast.makeText(context, "در حال اتصال به گفتگو...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
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

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Order Details Summary Card
                AnimatedVisibility(visible = showOrderDetails) {
                    OrderDetailsSummaryCard(request = request)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    state = listState,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(messages.reversed(), key = { it.id }) { message ->
                        if (message.type == "form") {
                            val form = formRequests.find { it.messageId == message.id }
                            val response = formResponses.find { it.requestId == form?.id }
                            if (form != null) {
                                FormRequestCard(form, response) { data ->
                                    scope.launch {
                                        repository.submitFormResponse(form.id, request.id, data)
                                    }
                                }
                            } else {
                                // Fallback if form not loaded yet
                                Text("در حال بارگذاری فرم...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                            }
                        } else {
                            MessageBubble(message)
                        }
                    }
                }
            }
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
            .padding(16.dp),
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

                // Show Invoice if issued
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

                // Show Admin Info if order is accepted
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
fun ChatBottomBar(
    isClosed: Boolean,
    isSubmitted: Boolean,
    conversationId: String,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    status: ServiceStatus,
    result: String?,
    onIUnderstood: () -> Unit
) {
    Surface(
        color = Color(0xFF1B1B2F),
        tonalElevation = 8.dp,
        modifier = Modifier.imePadding()
    ) {
        if (status == ServiceStatus.REJECTED && result == "expired_without_acceptance") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onIUnderstood,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("فهمیدم", fontFamily = Lalezar, fontSize = 16.sp)
                }
            }
        } else if (!isClosed && !isSubmitted && conversationId.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* File picker */ }) {
                    Icon(Icons.Default.AttachFile, null, tint = Color.White.copy(alpha = 0.6f))
                }
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("اینجا بنویسید...", fontFamily = Vazir, fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = Color(0xFF6C5CE7),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                }
            }
        } else {
            val footerText = when {
                isClosed -> "این گفتگو بسته شده است."
                isSubmitted -> "درخواست شما ثبت شده و بزودی گفتگو با کارشناس آغاز می‌شود."
                conversationId.isNotEmpty() -> "کارشناسان فرم های مربوط را برای شما ارسال می کنند."
                else -> "در حال آماده‌سازی گفتگو..."
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = footerText,
                    fontFamily = Vazir,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isCustomer = message.senderRole == "CUSTOMER"
    val isSystem = message.senderRole == "SYSTEM"
    
    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    text = message.content,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = Vazir,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        return
    }

    val color = if (isCustomer) Color(0xFF6C5CE7) else Color.White.copy(alpha = 0.08f)
    val textColor = Color.White
    
    val time = SimpleDateFormat("HH:mm", Locale.forLanguageTag("fa")).format(Date(message.timestamp))
        .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isCustomer) 18.dp else 4.dp,
                        bottomEnd = if (isCustomer) 4.dp else 18.dp
                    )
                )
                .background(color)
                .padding(12.dp)
        ) {
            Text(text = message.content, color = textColor, fontFamily = Vazir, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                color = textColor.copy(alpha = 0.4f),
                fontFamily = Vazir,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
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
