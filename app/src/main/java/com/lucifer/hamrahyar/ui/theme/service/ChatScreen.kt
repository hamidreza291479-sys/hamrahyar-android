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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormResponseDto
import com.lucifer.hamrahyar.ui.theme.data.model.InvoiceDto
import com.lucifer.hamrahyar.ui.theme.data.model.InvoiceItemDto
import com.lucifer.hamrahyar.ui.theme.data.model.PaymentDto
import com.lucifer.hamrahyar.ui.theme.domain.model.*
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.utils.PersianDateUtil
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
    val scope = rememberCoroutineScope()
    
    val conversationId = request.conversationId ?: ""
    val profileId = request.profileId ?: ""
    val orderId = request.id
    
    val chatViewModel: ChatViewModel = viewModel(
        key = "chat_$conversationId",
        factory = ChatViewModelFactory(repository, conversationId, profileId, orderId)
    )
    
    val uiState by chatViewModel.uiState.collectAsState()
    
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    val isClosed = request.status == ServiceStatus.CLOSED || request.status == ServiceStatus.ARCHIVED || request.status == ServiceStatus.CANCELLED || request.status == ServiceStatus.REJECTED
    val isSubmitted = request.status == ServiceStatus.SUBMITTED

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
                        visible = uiState.realtimeStatus != "CONNECTED",
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

            if (showPaymentDialog && uiState.invoice != null) {
                CardToCardPaymentDialog(
                    invoice = uiState.invoice!!,
                    defaultPayerName = request.customerFullName ?: "",
                    onDismiss = { showPaymentDialog = false },
                    onSubmit = { payerName, bankName, cardLast4, trackingCode ->
                        val invoice = uiState.invoice!!
                        val amount = if (invoice.totalAmount > 0) invoice.totalAmount else invoice.amount
                        chatViewModel.submitCardToCardPayment(
                            invoiceId = invoice.id,
                            amount = amount,
                            payerFullName = payerName,
                            payerBank = bankName,
                            payerCardLast4 = cardLast4,
                            paymentTrackingCode = trackingCode,
                            receiptPath = null
                        ) { result ->
                            result.onSuccess {
                                Toast.makeText(context, "اطلاعات پرداخت با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                                showPaymentDialog = false
                            }.onFailure { error ->
                                Toast.makeText(context, error.message ?: "خطا در ثبت پرداخت", Toast.LENGTH_SHORT).show()
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.invoice != null) {
                    item {
                        InvoiceCard(
                            invoice = uiState.invoice!!,
                            payment = uiState.payment,
                            onPayClick = { showPaymentDialog = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    Text(
                        "فرم‌های مورد نیاز",
                        fontFamily = Lalezar,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                if (uiState.isLoading && uiState.formRequests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFa29bfe))
                        }
                    }
                } else if (uiState.formRequests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "کارشناسان فرم‌های مربوط را برای شما ارسال می‌کنند.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = Vazir,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(uiState.formRequests, key = { "form_${it.id}" }) { form ->
                    val response = uiState.formResponses.find { it.requestId == form.id }
                    FormRequestCard(
                        request = form, 
                        response = response,
                        repository = repository,
                        orderId = request.id,
                        onSubmit = { data, onResult ->
                            chatViewModel.submitForm(form.id, request.id, data) { result ->
                                result.onSuccess {
                                    Toast.makeText(context, "فرم با موفقیت ارسال شد", Toast.LENGTH_SHORT).show()
                                }.onFailure { error ->
                                    Toast.makeText(context, error.message ?: "خطا در ارسال فرم", Toast.LENGTH_SHORT).show()
                                }
                                onResult(result)
                            }
                        }
                    )
                }
                
                val notices = uiState.messages.filter { it.senderRole == "ADMIN" && it.type == "text" }
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
fun InvoiceCard(
    invoice: InvoiceDto,
    payment: PaymentDto?,
    onPayClick: () -> Unit
) {
    val totalAmount = if (invoice.totalAmount > 0) invoice.totalAmount else invoice.amount

    val formattedAmount = try {
        val longVal = totalAmount.toLong()
        java.text.NumberFormat.getNumberInstance(Locale.US).format(longVal)
            .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴")
            .replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
    } catch (e: Exception) {
        totalAmount.toString()
    }

    val issueDateText = PersianDateUtil.parseIsoToPersianText(invoice.createdAt, includeTime = true)

    val invoiceStatusLabel = when (invoice.status.lowercase()) {
        "paid" -> "پرداخت شده"
        "waiting_payment", "unpaid", "pending" -> "در انتظار پرداخت"
        "cancelled" -> "لغو شده"
        "processing" -> "در حال پردازش"
        else -> invoice.status
    }

    val invoiceStatusColor = when (invoice.status.lowercase()) {
        "paid" -> Color(0xFF00B894)
        "waiting_payment", "unpaid", "pending" -> Color(0xFFE17055)
        "cancelled" -> Color(0xFFD63031)
        else -> Color(0xFF0984E3)
    }

    val paymentStatusLabel = when (payment?.status?.lowercase()) {
        "pending", "pending_review" -> "پرداخت در انتظار بررسی"
        "approved", "success" -> "پرداخت تأیید شده"
        "rejected", "failed" -> "پرداخت رد شده"
        else -> null
    }

    val paymentStatusColor = when (payment?.status?.lowercase()) {
        "pending", "pending_review" -> Color(0xFFFDCB6E)
        "approved", "success" -> Color(0xFF00B894)
        "rejected", "failed" -> Color(0xFFD63031)
        else -> Color.Gray
    }

    val canPay = invoice.status.lowercase() in listOf("waiting_payment", "unpaid", "pending") &&
            payment?.status?.lowercase() !in listOf("pending", "pending_review", "approved", "success")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B3A)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF00CEC9).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF00CEC9), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فاکتور سفارش", fontFamily = Lalezar, fontSize = 17.sp, color = Color.White)
                }

                if (!invoice.invoiceNumber.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFF00CEC9).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = invoice.invoiceNumber,
                            fontFamily = Vazir,
                            fontSize = 11.sp,
                            color = Color(0xFF00CEC9),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (issueDateText.isNotBlank()) {
                    SummaryRow("تاریخ صدور", issueDateText)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("وضعیت فاکتور", color = Color.White.copy(alpha = 0.6f), fontFamily = Vazir, fontSize = 12.sp)
                    Surface(
                        color = invoiceStatusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, invoiceStatusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = invoiceStatusLabel,
                            color = invoiceStatusColor,
                            fontFamily = Vazir,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (paymentStatusLabel != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("وضعیت پرداخت", color = Color.White.copy(alpha = 0.6f), fontFamily = Vazir, fontSize = 12.sp)
                        Surface(
                            color = paymentStatusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, paymentStatusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = paymentStatusLabel,
                                color = paymentStatusColor,
                                fontFamily = Vazir,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (payment?.status?.lowercase() in listOf("rejected", "failed") && !payment?.rejectionReason.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFFF7675).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFFF7675).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("علت رد پرداخت:", color = Color(0xFFFF7675), fontFamily = Vazir, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(payment.rejectionReason, color = Color.White, fontFamily = Vazir, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (invoice.items.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                Text("آیتم‌های فاکتور", fontFamily = Lalezar, fontSize = 14.sp, color = Color(0xFFa29bfe))
                Spacer(modifier = Modifier.height(6.dp))

                invoice.items.forEach { item ->
                    val itemAmtFormatted = try {
                        val longVal = item.amount.toLong()
                        java.text.NumberFormat.getNumberInstance(Locale.US).format(longVal)
                            .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴")
                            .replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
                    } catch (e: Exception) {
                        item.amount.toString()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color.White.copy(alpha = 0.9f), fontFamily = Vazir, fontSize = 12.sp)
                            if (!item.description.isNullOrBlank()) {
                                Text(item.description, color = Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 10.sp)
                            }
                        }
                        Text("$itemAmtFormatted تومان", color = Color.White, fontFamily = Vazir, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val hasCardInfo = !invoice.bankName.isNullOrBlank() || !invoice.cardNumber.isNullOrBlank() || !invoice.accountOwner.isNullOrBlank() || !invoice.accountTitle.isNullOrBlank()
            if (hasCardInfo) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                Surface(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("اطلاعات حساب مقصد جهت واریز", fontFamily = Lalezar, fontSize = 13.sp, color = Color(0xFFFDCB6E))
                        if (!invoice.bankName.isNullOrBlank()) {
                            SummaryRow("بانک", invoice.bankName)
                        }
                        val owner = invoice.accountOwner ?: invoice.accountTitle
                        if (!owner.isNullOrBlank()) {
                            SummaryRow("صاحب حساب", owner)
                        }
                        if (!invoice.cardNumber.isNullOrBlank()) {
                            SummaryRow("شماره کارت", invoice.cardNumber)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مبلغ نهایی فاکتور", fontFamily = Lalezar, fontSize = 15.sp, color = Color.White)
                Text(
                    text = "$formattedAmount تومان",
                    fontFamily = Lalezar,
                    fontSize = 18.sp,
                    color = Color(0xFF00CEC9),
                    fontWeight = FontWeight.Bold
                )
            }

            if (canPay) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CEC9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("پرداخت کارت به کارت", fontFamily = Lalezar, fontSize = 15.sp, color = Color(0xFF0F0C29))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardToCardPaymentDialog(
    invoice: InvoiceDto,
    defaultPayerName: String,
    onDismiss: () -> Unit,
    onSubmit: (payerName: String, bankName: String, cardLast4: String, trackingCode: String) -> Unit
) {
    var payerName by remember { mutableStateOf(defaultPayerName) }
    var bankName by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }
    var trackingCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val totalAmount = if (invoice.totalAmount > 0) invoice.totalAmount else invoice.amount
    val formattedAmount = try {
        val longVal = totalAmount.toLong()
        java.text.NumberFormat.getNumberInstance(Locale.US).format(longVal)
            .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴")
            .replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
    } catch (e: Exception) {
        totalAmount.toString()
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = Color(0xFF1B1B2F),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("ثبت پرداخت کارت به کارت", fontFamily = Lalezar, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!invoice.cardNumber.isNullOrBlank() || !invoice.bankName.isNullOrBlank()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("مقصد واریز:", fontFamily = Lalezar, fontSize = 12.sp, color = Color(0xFFFDCB6E))
                            if (!invoice.bankName.isNullOrBlank()) {
                                Text("بانک: ${invoice.bankName}", fontFamily = Vazir, fontSize = 11.sp, color = Color.White)
                            }
                            val owner = invoice.accountOwner ?: invoice.accountTitle
                            if (!owner.isNullOrBlank()) {
                                Text("به نام: $owner", fontFamily = Vazir, fontSize = 11.sp, color = Color.White)
                            }
                            if (!invoice.cardNumber.isNullOrBlank()) {
                                Text("شماره کارت: ${invoice.cardNumber}", fontFamily = Vazir, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text(
                    text = "مبلغ قابل پرداخت: $formattedAmount تومان",
                    fontFamily = Vazir,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00CEC9)
                )

                OutlinedTextField(
                    value = payerName,
                    onValueChange = { payerName = it },
                    label = { Text("نام و نام خانوادگی واریزکننده", fontFamily = Vazir, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00CEC9),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF00CEC9),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک واریزکننده (مثلا: ملی، سامان)", fontFamily = Vazir, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00CEC9),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF00CEC9),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = cardLast4,
                    onValueChange = { if (it.length <= 4) cardLast4 = it },
                    label = { Text("۴ رقم آخر کارت واریزکننده", fontFamily = Vazir, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00CEC9),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF00CEC9),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = { trackingCode = it },
                    label = { Text("شماره پیگیری / کد ارجاع", fontFamily = Vazir, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00CEC9),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF00CEC9),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF7675),
                        fontFamily = Vazir,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (payerName.isBlank()) {
                        errorMessage = "لطفاً نام واریزکننده را وارد کنید"
                        return@Button
                    }
                    if (bankName.isBlank()) {
                        errorMessage = "لطفاً نام بانک واریزکننده را وارد کنید"
                        return@Button
                    }
                    if (cardLast4.length != 4) {
                        errorMessage = "۴ رقم آخر کارت باید دقیقاً ۴ رقم باشد"
                        return@Button
                    }
                    if (trackingCode.isBlank()) {
                        errorMessage = "لطفاً کد پیگیری را وارد کنید"
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true
                    onSubmit(payerName.trim(), bankName.trim(), cardLast4.trim(), trackingCode.trim())
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CEC9))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("ثبت و ارسال پرداخت", fontFamily = Vazir, color = Color(0xFF0F0C29), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("انصراف", color = Color.White, fontFamily = Vazir)
            }
        }
    )
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
