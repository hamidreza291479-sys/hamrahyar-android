package com.lucifer.hamrahyar.ui.theme.service

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.home.components.PlateInput
import com.lucifer.hamrahyar.ui.theme.utils.PlateUtils
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormResponseDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormField
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Composable
fun FormRequestCard(
    request: FormRequestDto,
    response: FormResponseDto? = null,
    repository: OnlineServiceRepository,
    orderId: String,
    onSubmit: (String, onResult: (Result<Unit>) -> Unit) -> Unit
) {
    val isSubmitted = response != null || request.status == "submitted"
    val isExpired = request.status == "expired"
    val json = remember { Json { ignoreUnknownKeys = true } }
    val scope = rememberCoroutineScope()
    
    val fields = remember(request.schema) {
        val fieldsElement = request.schema?.get("fields")
        if (fieldsElement is JsonArray) {
            fieldsElement.mapNotNull { 
                try { json.decodeFromJsonElement<FormField>(it) } catch(e: Exception) { null }
            }
        } else emptyList()
    }

    val formValues = remember { mutableStateMapOf<String, String>() }
    var isUploading by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<String?>(null) }
    
    // Initialize from response if submitted
    LaunchedEffect(response, request.response) {
        val serverResponse = response?.data ?: request.response
        if (isSubmitted && serverResponse != null) {
            serverResponse.forEach { (k, v) ->
                if (v is JsonPrimitive) formValues[k] = v.content
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubmitted) Color(0xFF00B894).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isSubmitted) Color(0xFF00B894).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSubmitted) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
                    contentDescription = null,
                    tint = if (isSubmitted) Color(0xFF00B894) else Color(0xFFa29bfe),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = request.prompt,
                    fontFamily = Lalezar,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isSubmitted) {
                SubmittedFormView(fields, formValues, request.submittedAt)
            } else if (isExpired) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "این فرم منقضی شده است و دیگر قابل تکمیل نیست.", 
                        color = Color(0xFFFF7675), 
                        fontFamily = Vazir, 
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (request.type == "timed") {
                TimedForm(request) { data ->
                    if (!isSubmitting) {
                        isSubmitting = true
                        onSubmit(data) { result ->
                            if (result.isFailure) isSubmitting = false
                        }
                    }
                }
            } else if (fields.isNotEmpty()) {
                DynamicFormRenderer(
                    fields = fields,
                    formValues = formValues,
                    repository = repository,
                    orderId = orderId,
                    formRequestId = request.id,
                    isUploading = isUploading || isSubmitting,
                    onUploadingChange = { isUploading = it },
                    onProgressChange = { uploadProgress = it },
                    onSubmit = { 
                        if (isSubmitting) return@DynamicFormRenderer
                        isSubmitting = true
                        val finalData = buildJsonObject { 
                            formValues.forEach { (k, v) -> 
                                val matchingField = fields.find { it.realKey == k }
                                val canonicalFieldType = matchingField?.type?.trim()?.lowercase()?.replace("-", "_")
                                val processedValue = if (PlateUtils.isPlateType(canonicalFieldType)) {
                                    PlateUtils.clean(v)
                                } else if (canonicalFieldType == "numeric" || canonicalFieldType == "number") {
                                    v.map { c ->
                                        when (c) {
                                            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'; '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
                                            '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'; '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                                            else -> c
                                        }
                                    }.joinToString("").replace("\\s+".toRegex(), "")
                                } else {
                                    v
                                }
                                put(k, processedValue)
                            }
                        }.toString()
                        
                        onSubmit(finalData) { result ->
                            if (result.isFailure) {
                                isSubmitting = false
                            }
                            // On success, the card will eventually recompose when request.status changes
                        }
                    }
                )
                
                if ((isUploading || isSubmitting) && uploadProgress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFa29bfe))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSubmitting && !isUploading) "در حال ارسال..." else uploadProgress!!, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = Vazir)
                    }
                }
            } else {
                LegacyFormSwitcher(request) { data ->
                    if (!isSubmitting) {
                        isSubmitting = true
                        onSubmit(data) { result ->
                            if (result.isFailure) isSubmitting = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubmittedFormView(fields: List<FormField>, formValues: Map<String, String>, submittedAt: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("پاسخ شما با موفقیت ثبت شده است:", color = Color(0xFF00B894), fontFamily = Vazir, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        
        if (submittedAt != null) {
            val date = try {
                val instant = kotlinx.datetime.Instant.parse(submittedAt)
                val javaDate = Date(instant.toEpochMilliseconds())
                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.forLanguageTag("fa")).format(javaDate)
                    .replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
            } catch (e: Exception) { null }
            
            if (date != null) {
                Text("زمان ثبت: $date", color = Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fields.isNotEmpty()) {
                    fields.forEach { field ->
                        val value = formValues[field.realKey] ?: "-"
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(field.label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = Vazir)
                            Text(value, color = Color.White, fontSize = 12.sp, fontFamily = Vazir, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "اطلاعات با موفقیت ارسال شد.",
                        color = Color.White,
                        fontFamily = Vazir,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicFormRenderer(
    fields: List<FormField>,
    formValues: MutableMap<String, String>,
    repository: OnlineServiceRepository,
    orderId: String,
    formRequestId: String,
    isUploading: Boolean,
    onUploadingChange: (Boolean) -> Unit,
    onProgressChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        fields.forEach { field ->
            DynamicField(
                field = field, 
                formValues = formValues, 
                repository = repository,
                orderId = orderId,
                formRequestId = formRequestId,
                onUploadingChange = onUploadingChange,
                onProgressChange = onProgressChange
            )
        }
        
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
            enabled = !isUploading && isDynamicFormValid(fields, formValues),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
        ) {
            Text("ارسال فرم", fontFamily = Lalezar, fontSize = 16.sp)
        }
    }
}

fun isDynamicFormValid(fields: List<FormField>, formValues: Map<String, String>): Boolean {
    return fields.all { field ->
        val canonicalType = field.type.trim().lowercase().replace("-", "_")
        val value = formValues[field.realKey]

        // Rule 5 & 8: delivery_method MUST have options if required or if value is provided
        if (canonicalType == "delivery_method" && field.options.isNullOrEmpty()) return false

        if (field.required) {
            if (value.isNullOrBlank()) return false
            // Rule 8 validation
            when (canonicalType) {
                "numeric", "number" -> if (!value.all { it.isDigit() }) return false
                "timed", "time" -> if (!value.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) return false
                "delivery_method" -> if (field.options?.contains(value) != true) return false
                "file" -> if (value.isBlank()) return false
                "plate", "plate_vehicle", "plate_motorcycle", "vehicle_plate", "motorcycle_plate" -> if (value.isBlank()) return false
            }
        } else if (!value.isNullOrBlank()) {
            when (canonicalType) {
                "numeric", "number" -> if (!value.all { it.isDigit() }) return false
                "timed", "time" -> if (!value.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) return false
                "delivery_method" -> if (field.options?.contains(value) != true) return false
            }
        }
        true
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicField(
    field: FormField, 
    formValues: MutableMap<String, String>,
    repository: OnlineServiceRepository,
    orderId: String,
    formRequestId: String,
    onUploadingChange: (Boolean) -> Unit,
    onProgressChange: (String) -> Unit
) {
    val value = formValues[field.realKey] ?: ""
    val label = field.label + if (field.required) " *" else ""
    val canonicalType = remember(field.type) { field.type.trim().lowercase().replace("-", "_") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontFamily = Vazir, modifier = Modifier.padding(bottom = 6.dp))
        
        when (canonicalType) {
            "text", "numeric", "number" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = { formValues[field.realKey] = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(field.placeholder ?: "وارد کنید...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = if (canonicalType == "numeric" || canonicalType == "number") KeyboardType.Number else KeyboardType.Text),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
            "file" -> {
                FilePickerField(
                    field = field,
                    currentValue = value,
                    repository = repository,
                    orderId = orderId,
                    formRequestId = formRequestId,
                    onFileUploaded = { fileId -> formValues[field.realKey] = fileId },
                    onUploadingChange = onUploadingChange,
                    onProgressChange = onProgressChange
                )
            }
            "plate", "plate_vehicle", "vehicle_plate" -> {
                PlateInput(plateType = "خودرو", initialValue = value, onValueChange = { formValues[field.realKey] = it }, label = label)
            }
            "plate_motorcycle", "motorcycle_plate" -> {
                PlateInput(plateType = "موتور", initialValue = value, onValueChange = { formValues[field.realKey] = it }, label = label)
            }
            "time" -> {
                var showTimePicker by remember { mutableStateOf(false) }
                val currentHour = if (value.contains(":")) value.split(":")[0].toIntOrNull() ?: 12 else 12
                val currentMinute = if (value.contains(":")) value.split(":")[1].toIntOrNull() ?: 0 else 0
                
                val timePickerState = rememberTimePickerState(
                    initialHour = currentHour,
                    initialMinute = currentMinute,
                    is24Hour = true
                )

                Surface(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, null, tint = Color(0xFFa29bfe))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (value.isEmpty()) "انتخاب زمان (۲۴ ساعته)..." else value,
                            color = if (value.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                            fontSize = 14.sp,
                            fontFamily = Vazir
                        )
                    }
                }

                if (showTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val formattedTime = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                                formValues[field.realKey] = formattedTime
                                showTimePicker = false
                            }) { Text("تأیید", fontFamily = Vazir) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) { Text("لغو", fontFamily = Vazir) }
                        },
                        text = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TimePicker(
                                    state = timePickerState,
                                    colors = TimePickerDefaults.colors(
                                        clockDialColor = Color.White.copy(alpha = 0.05f),
                                        selectorColor = Color(0xFF6C5CE7),
                                        containerColor = Color(0xFF2D3436),
                                        periodSelectorSelectedContainerColor = Color(0xFF6C5CE7)
                                    )
                                )
                            }
                        }
                    )
                }
            }
            "timed" -> {
                // Rendered as OTP field in dynamic form if present
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> 
                        val filtered = input.filter { it.isDigit() || "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩".contains(it) }
                        formValues[field.realKey] = filtered
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("کد تایید", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
            "delivery_method" -> {
                if (field.options.isNullOrEmpty()) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "این فرم گزینه تحویل ندارد. لطفا با پشتیبانی تماس بگیرید.",
                            color = Color(0xFFFF7675),
                            fontFamily = Vazir,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("انتخاب نحوه تحویل...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFa29bfe),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2D3436))
                        ) {
                            field.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontFamily = Vazir, color = Color.White) },
                                    onClick = {
                                        formValues[field.realKey] = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Text("نوع فیلد '${field.type}' پشتیبانی نمی‌شود.", color = Color.Gray, fontSize = 11.sp, fontFamily = Vazir)
            }
        }
        
        if (!field.description.isNullOrBlank()) {
            Text(field.description!!, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = Vazir, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun FilePickerField(
    field: FormField,
    currentValue: String,
    repository: OnlineServiceRepository,
    orderId: String,
    formRequestId: String,
    onFileUploaded: (String) -> Unit,
    onUploadingChange: (Boolean) -> Unit,
    onProgressChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    onUploadingChange(true)
                    onProgressChange("در حال بررسی فایل...")
                    
                    val contentResolver = context.contentResolver
                    var fileName = "document"
                    var fileSize = 0L
                    
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                    
                    // Validation
                    val maxMb = field.maxSizeMb ?: 5
                    if (fileSize > maxMb * 1024 * 1024) {
                        Toast.makeText(context, "حجم فایل بیش از حد مجاز ($maxMb مگابایت) است.", Toast.LENGTH_LONG).show()
                        onUploadingChange(false)
                        return@launch
                    }
                    
                    selectedFileName = fileName
                    onProgressChange("در حال آپلود فایل...")
                    
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: byteArrayOf()
                    inputStream?.close()
                    
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    val extension = fileName.substringAfterLast(".", "")
                    val uniqueName = "${UUID.randomUUID()}.$extension"
                    val storagePath = "$orderId/$formRequestId/$uniqueName"
                    
                        repository.uploadFile("service-files", storagePath, bytes, mimeType).onSuccess { path ->
                        onProgressChange("در حال ثبت اطلاعات...")
                        repository.registerFormFile(formRequestId, fileName, path, mimeType, fileSize).onSuccess { fileId ->
                            onFileUploaded(fileId)
                            onProgressChange("فایل با موفقیت ثبت شد")
                            onUploadingChange(false)
                        }.onFailure { err ->
                            Log.e("FilePicker", "FILE_REGISTER_FAILED (metadata failure): ${err.message}", err)
                            Toast.makeText(context, "خطا در ثبت متادیتای فایل: ${err.message}", Toast.LENGTH_SHORT).show()
                            onUploadingChange(false)
                        }
                    }.onFailure { err ->
                        val code = err.message ?: ""
                        val persianMsg = when (code) {
                            "unauthenticated" -> "خطای احراز هویت. لطفاً ابتدا وارد شوید."
                            "anonymous_session" -> "نشست کاربری شما معتبر نیست. لطفاً اطلاعات فردی خود را تأیید کنید."
                            "storage_policy_denied" -> "عدم دسترسی سیاست امنیتی Storage برای این فایل."
                            "invalid_path" -> "مسیر فایل نامعتبر است."
                            "form_not_pending" -> "فرم ارسالی دیگر در وضعیت معلق نیست."
                            "order_not_owned" -> "این سفارش متعلق به شناسه کاربری شما نیست."
                            "file_too_large" -> "حجم فایل بیشتر از حد مجاز است."
                            "invalid_file_type" -> "فایل انتخاب شده نامعتبر یا خالی است."
                            "network_error" -> "خطای شبکه. لطفاً اتصال اینترنت خود را بررسی کنید."
                            else -> "آپلود فایل انجام نشد ($code). لطفاً دوباره تلاش کنید."
                        }
                        Log.e("FilePicker", "FILE_UPLOAD_FAILED: code=$code")
                        Toast.makeText(context, persianMsg, Toast.LENGTH_LONG).show()
                        onUploadingChange(false)
                    }
                } catch (e: Exception) {
                    Log.e("FilePicker", "FILE_REGISTER_FAILED (catch block)")
                    Toast.makeText(context, "خطای غیرمنتظره در پردازش فایل", Toast.LENGTH_SHORT).show()
                    onUploadingChange(false)
                }
            }
        }
    }

    Surface(
        onClick = { if (!currentValue.startsWith("FILE_")) launcher.launch(arrayOf("*/*")) },
        enabled = currentValue.isEmpty(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (currentValue.isNotEmpty()) Icons.Rounded.CheckCircle else Icons.Rounded.CloudUpload, 
                contentDescription = null, 
                tint = if (currentValue.isNotEmpty()) Color(0xFF00B894) else Color(0xFFa29bfe)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    selectedFileName ?: if (currentValue.isNotEmpty()) "فایل با موفقیت انتخاب شد" else "انتخاب فایل...",
                    color = if (selectedFileName == null && currentValue.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                    fontSize = 14.sp,
                    fontFamily = Vazir
                )
                if (currentValue.isNotEmpty()) {
                    Text("آماده ارسال", color = Color(0xFF00B894), fontSize = 10.sp, fontFamily = Vazir)
                }
            }
        }
    }
}

@Composable
fun LegacyFormSwitcher(request: FormRequestDto, onSubmit: (String) -> Unit) {
    val canonicalType = remember(request.type) { request.type.trim().lowercase().replace("-", "_") }
    when (canonicalType) {
        "numeric", "number" -> NumericForm(onSubmit)
        "vehicle_plate", "plate_vehicle" -> PlateForm("car", onSubmit)
        "motorcycle_plate", "plate_motorcycle" -> PlateForm("motor", onSubmit)
        "text" -> TextForm(onSubmit)
        "timed", "time" -> TimedForm(request, onSubmit)
        "delivery_method" -> DeliveryMethodForm(onSubmit)
        "invoice" -> InvoiceFormView(request, onSubmit)
        "file" -> LegacyFileForm(request, onSubmit)
        "card_to_card_payment" -> CardPaymentForm(request, onSubmit)
        "survey" -> SurveyForm(request, onSubmit)
        "end_service_summary" -> ServiceSummaryView(request, onSubmit)
        else -> {
            Text("نوع فرم ناشناخته: ${request.type}", color = Color.Gray, fontSize = 12.sp)
        }
    }
}


// Re-using existing legacy forms but wrapping them for compatibility

@Composable
fun NumericForm(onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { input -> if (input.all { it.isDigit() || "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩".contains(it) }) value = input },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("فقط عدد وارد کنید", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Button(
            onClick = { 
                val normalized = value.map { c ->
                    when (c) {
                        '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'; '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
                        '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'; '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                        else -> c
                    }
                }.joinToString("").replace("\\s+".toRegex(), "")
                onSubmit(buildJsonObject { put("value", normalized) }.toString()) 
            },
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
            onClick = { 
                val cleanedPlate = PlateUtils.clean(plateValue)
                onSubmit(buildJsonObject { put("plate", cleanedPlate) }.toString()) 
            },
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
    val codeLength = remember(request.schema) {
        val len = request.schema?.get("code_length")?.jsonPrimitive?.intOrNull ?: 6
        len.coerceIn(4, 8)
    }
    var timeLeft by remember { mutableLongStateOf(0L) }
    var expired by remember { mutableStateOf(false) }

    val timeManager = com.lucifer.hamrahyar.ui.theme.utils.TimeManager

    LaunchedEffect(request.expiresAt) {
        val expiry = request.expiresAt?.let { 
            try { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() } catch(e: Exception) { 0L }
        } ?: 0L
        
        if (expiry == 0L) return@LaunchedEffect
        
        while (expiry > timeManager.getCurrentTimeMillis()) {
            timeLeft = (expiry - timeManager.getCurrentTimeMillis()) / 1000
            delay(1000)
        }
        expired = true
    }

    var otpValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = if (expired) Color(0xFFFF7675) else Color(0xFFFDCB6E), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            if (!expired && timeLeft > 0) {
                val min = timeLeft / 60
                val sec = timeLeft % 60
                Text(
                    text = "زمان باقی‌مانده: ${String.format(Locale.US, "%02d:%02d", min, sec)}",
                    color = Color(0xFFFDCB6E),
                    fontFamily = Vazir,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "زمان اعتبار این رمز به پایان رسیده است",
                    color = Color(0xFFFF7675),
                    fontFamily = Vazir,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // OTP Input implementation with multiple boxes
        val digits = remember(otpValue, codeLength) {
            List(codeLength) { i -> otpValue.getOrNull(i)?.toString() ?: "" }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Hidden TextField to capture input
            androidx.compose.foundation.text.BasicTextField(
                value = otpValue,
                onValueChange = { input ->
                    if (expired) return@BasicTextField
                    val normalized = input.map { c ->
                        when (c) {
                            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'; '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
                            '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'; '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                            else -> c
                        }
                    }.filter { it.isDigit() }.joinToString("")
                    
                    if (normalized.length <= codeLength) {
                        otpValue = normalized
                        if (normalized.length == codeLength) {
                            focusManager.clearFocus()
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.alpha(0.01f).size(1.dp), // Hidden but focusable
                enabled = !expired
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    // Logic to request focus on the hidden field is implicit if it's the only one
                }
            ) {
                for (i in 0 until codeLength) {
                    val char = digits[i]
                    val isFocused = otpValue.length == i && !expired
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 50.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFFa29bfe) else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = Vazir
                        )
                        if (isFocused) {
                            // Simple cursor blink simulation
                            Box(modifier = Modifier.width(2.dp).height(20.dp).background(Color(0xFFa29bfe)))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                onSubmit(buildJsonObject { put("value", otpValue) }.toString())
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !expired && otpValue.length == codeLength,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
        ) {
            Text("تأیید و ارسال رمز", fontFamily = Lalezar, fontSize = 16.sp)
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
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFa29bfe) else Color.White.copy(alpha = 0.1f))
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
    val schemaObj = request.schema
    val invoiceObj = schemaObj?.get("invoice")?.jsonObject ?: schemaObj

    val total = invoiceObj?.get("amount")?.jsonPrimitive?.content 
        ?: invoiceObj?.get("total_amount")?.jsonPrimitive?.content 
        ?: invoiceObj?.get("total")?.jsonPrimitive?.content 
        ?: "0"

    val invoiceNumber = invoiceObj?.get("invoice_number")?.jsonPrimitive?.content 
        ?: request.response?.get("invoice_number")?.jsonPrimitive?.content

    val itemsArray = invoiceObj?.get("items")?.jsonArray

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (!invoiceNumber.isNullOrBlank()) {
            Text("شماره فاکتور: $invoiceNumber", color = Color(0xFF00CEC9), fontWeight = FontWeight.Bold, fontFamily = Vazir, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (itemsArray != null && itemsArray.isNotEmpty()) {
            Text("جزئیات فاکتور:", color = Color.White.copy(alpha = 0.7f), fontFamily = Vazir, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
            itemsArray.forEach { itemElement ->
                val itemObj = itemElement as? JsonObject
                if (itemObj != null) {
                    val title = itemObj["title"]?.jsonPrimitive?.content ?: ""
                    val itemType = itemObj["item_type"]?.jsonPrimitive?.content ?: ""
                    val itemAmt = itemObj["amount"]?.jsonPrimitive?.content ?: "0"
                    val itemDesc = itemObj["description"]?.jsonPrimitive?.content

                    val label = when (itemType.lowercase()) {
                        "fixed" -> "هزینه ثابت خدمت"
                        "speed" -> "هزینه سرعت"
                        "discount" -> "تخفیف"
                        "manual" -> title.ifBlank { "هزینه متغیر" }
                        else -> title.ifBlank { "آیتم فاکتور" }
                    }

                    val formattedAmt = try {
                        val amtLong = itemAmt.toDouble().toLong()
                        java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amtLong)
                    } catch (e: Exception) {
                        itemAmt
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = Color.White.copy(alpha = 0.8f), fontFamily = Vazir, fontSize = 12.sp)
                            if (!itemDesc.isNullOrBlank()) {
                                Text(itemDesc, color = Color.White.copy(alpha = 0.5f), fontFamily = Vazir, fontSize = 10.sp)
                            }
                        }
                        Text("$formattedAmt تومان", color = if (itemType.lowercase() == "discount") Color(0xFFFF7675) else Color.White, fontFamily = Vazir, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
        }

        val formattedTotal = try {
            val totalLong = total.toDouble().toLong()
            java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(totalLong)
        } catch (e: Exception) {
            total
        }

        Text("مبلغ نهایی فاکتور: $formattedTotal تومان", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = Vazir, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (request.status == "submitted" || request.response != null) {
            Text("این فاکتور تایید/پرداخت شده است.", color = Color(0xFF00B894), fontFamily = Vazir, fontSize = 14.sp)
        } else {
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
}

@Composable
fun LegacyFileForm(request: FormRequestDto, onSubmit: (String) -> Unit) {
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
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
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
    
    val schemaObj = request.schema
    val invoiceObj = schemaObj?.get("invoice")?.jsonObject ?: schemaObj

    val cardNumber = invoiceObj?.get("card_number")?.jsonPrimitive?.content
    val accountOwner = invoiceObj?.get("account_owner")?.jsonPrimitive?.content
    val bankName = invoiceObj?.get("bank_name")?.jsonPrimitive?.content
    val accountTitle = invoiceObj?.get("account_title")?.jsonPrimitive?.content

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!cardNumber.isNullOrBlank() || !accountOwner.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("اطلاعات کارت واریز مقصد:", color = Color(0xFF00CEC9), fontWeight = FontWeight.Bold, fontFamily = Vazir, fontSize = 13.sp)
                    
                    if (!bankName.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نام بانک:", color = Color.White.copy(alpha = 0.6f), fontFamily = Vazir, fontSize = 12.sp)
                            Text(bankName, color = Color.White, fontFamily = Vazir, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!accountTitle.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("عنوان حساب:", color = Color.White.copy(alpha = 0.6f), fontFamily = Vazir, fontSize = 12.sp)
                            Text(accountTitle, color = Color.White, fontFamily = Vazir, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!accountOwner.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نام صاحب حساب:", color = Color.White.copy(alpha = 0.6f), fontFamily = Vazir, fontSize = 12.sp)
                            Text(accountOwner, color = Color.White, fontFamily = Vazir, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!cardNumber.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("شماره کارت:", color = Color.White.copy(alpha = 0.6f), fontFamily = Vazir, fontSize = 12.sp)
                            Text(cardNumber, color = Color(0xFFa29bfe), fontFamily = Vazir, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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
