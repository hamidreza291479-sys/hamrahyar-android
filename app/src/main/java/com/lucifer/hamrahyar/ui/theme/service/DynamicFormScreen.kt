package com.lucifer.hamrahyar.ui.theme.service

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.utils.ProvinceCityData
import com.lucifer.hamrahyar.ui.theme.utils.PlateUtils
import com.lucifer.hamrahyar.ui.home.components.PlateInput
import com.lucifer.hamrahyar.ui.theme.data.model.FormField
import com.lucifer.hamrahyar.ui.theme.data.model.FormCondition
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormScreen(
    serviceId: String,
    repository: OnlineServiceRepository,
    initialData: String? = null,
    errors: List<com.lucifer.hamrahyar.ui.theme.data.model.FormFieldError>? = null,
    onFormSubmit: (String) -> Unit,
    onBack: () -> Unit
) {
    var schemaObj by remember { mutableStateOf<JsonObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isReviewMode by remember { mutableStateOf(false) }
    val formValues = remember { mutableStateMapOf<String, String>() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    // Initialize from initialData
    LaunchedEffect(initialData) {
        if (!initialData.isNullOrBlank()) {
            try {
                val obj = Json.parseToJsonElement(initialData).jsonObject
                obj.forEach { (k, v) ->
                    if (v is JsonPrimitive) formValues[k] = v.content
                }
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(serviceId) {
        val schemaString = repository.getServiceForm(serviceId)
        if (schemaString != null) {
            schemaObj = Json.parseToJsonElement(schemaString).jsonObject
        }
        isLoading = false
    }

    val fields = remember(schemaObj) {
        val fieldsElement = schemaObj?.get("fields")
        if (fieldsElement is JsonArray) {
            fieldsElement.mapNotNull { 
                try { json.decodeFromJsonElement<FormField>(it) } catch(e: Exception) { null }
            }
        } else emptyList()
    }
    
    // If we have errors, we shouldn't be in review mode
    LaunchedEffect(errors) {
        if (errors != null) isReviewMode = false
    }

    // Evaluate visibility
    val visibleFields = remember(formValues.toMap(), fields) {
        fields.filter { field ->
            val condition = field.visibleWhen
            if (condition == null) true
            else {
                val depValue = formValues[condition.field] ?: ""
                val targetValue = when (val eq = condition.equals) {
                    is JsonPrimitive -> eq.content
                    else -> eq.toString()
                }
                depValue == targetValue
            }
        }
    }
    
    // Clear values of hidden fields
    LaunchedEffect(visibleFields) {
        val visibleKeys = visibleFields.map { it.realKey }.toSet()
        val toRemove = formValues.keys.filter { it !in visibleKeys && it !in listOf("full_name", "phone", "email") }
        toRemove.forEach { formValues.remove(it) }
    }

    Box(
        modifier = Modifier.fillMaxSize().drawBehind {
            drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63))))
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (isReviewMode) isReviewMode = false else onBack() }, 
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(if (isReviewMode) "تأیید اطلاعات" else "اطلاعات تکمیلی", fontFamily = Lalezar, fontSize = 20.sp, color = Color.White)
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (fields.isEmpty()) {
                NoFormView(onFormSubmit)
            } else {
                if (isReviewMode) {
                    ReviewView(
                        visibleFields = visibleFields,
                        formValues = formValues,
                        onEdit = { isReviewMode = false },
                        onSubmit = { 
                            onFormSubmit(buildJsonObject { 
                                formValues.forEach { (k, v) -> 
                                    val field = visibleFields.find { it.realKey == k }
                                    val isPlate = PlateUtils.isPlateType(field?.type)
                                    put(k, if (isPlate) PlateUtils.clean(v) else v)
                                } 
                            }.toString()) 
                        }
                    )
                } else {
                    FormEditorView(
                        fields = visibleFields,
                        formValues = formValues,
                        errors = errors,
                        onSubmit = { isReviewMode = true }
                    )
                }
            }
        }
    }
}

@Composable
fun NoFormView(onFormSubmit: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Text(
                "برای این خدمت نیاز به وارد کردن اطلاعات اضافی نیست. مستقیماً می‌توانید درخواست را ثبت کنید.",
                fontFamily = Vazir, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(20.dp), lineHeight = 24.sp, textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onFormSubmit("{}") },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("ثبت نهایی و شروع گفتگو", fontFamily = Lalezar, fontSize = 18.sp)
        }
    }
}

@Composable
fun FormEditorView(
    fields: List<FormField>,
    formValues: MutableMap<String, String>,
    errors: List<com.lucifer.hamrahyar.ui.theme.data.model.FormFieldError>? = null,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            fields.forEach { field ->
                val fieldError = errors?.find { it.key == field.realKey }
                RenderField(field, formValues, fieldError)
                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
        
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
            shape = RoundedCornerShape(16.dp),
            enabled = isFormValid(fields, formValues)
        ) {
            Text("بررسی و تأیید", fontFamily = Lalezar, fontSize = 18.sp)
        }
    }
}

fun isFormValid(fields: List<FormField>, formValues: Map<String, String>): Boolean {
    return fields.all { field ->
        val canonicalType = field.type.trim().lowercase().replace("-", "_")
        val isRequired = if (field.requiredWhen != null) {
            val depValue = formValues[field.requiredWhen.field] ?: ""
            val targetValue = when (val eq = field.requiredWhen.equals) {
                is JsonPrimitive -> eq.content
                else -> eq.toString()
            }
            depValue == targetValue
        } else field.required
        
        val value = formValues[field.realKey]
        
        if (isRequired) {
            if (value.isNullOrBlank() || value == "false") return false
            
            // Basic validation for special types if present in initial form
            when (canonicalType) {
                "number", "numeric" -> if (!value.all { it.isDigit() }) return false
                "time", "timed" -> if (!value.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) return false
                "plate", "plate_vehicle", "plate_motorcycle", "vehicle_plate", "motorcycle_plate" -> if (value.isBlank()) return false
            }
        } else if (!value.isNullOrBlank()) {
             when (canonicalType) {
                "number", "numeric" -> if (!value.all { it.isDigit() }) return false
                "time", "timed" -> if (!value.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) return false
            }
        }
        true
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderField(field: FormField, formValues: MutableMap<String, String>, error: com.lucifer.hamrahyar.ui.theme.data.model.FormFieldError? = null) {
    val value = formValues[field.realKey] ?: ""
    val label = field.label + if (field.required) " *" else ""
    val isError = error != null
    val canonicalType = remember(field.type) { field.type.trim().lowercase().replace("-", "_") }

    Column(modifier = Modifier.fillMaxWidth()) {
        when (canonicalType) {
            "text", "number", "numeric" -> {
                Text(label, fontFamily = Vazir, fontSize = 13.sp, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { formValues[field.realKey] = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = if (canonicalType == "number" || canonicalType == "numeric") KeyboardType.Number else KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = Color.Red
                    )
                )
            }
            "password" -> {
                var passwordVisible by remember { mutableStateOf(false) }
                Text(label, fontFamily = Vazir, fontSize = 13.sp, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { formValues[field.realKey] = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = isError,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = Color.Red
                    )
                )
            }
            "select", "delivery_method" -> {
                var expanded by remember { mutableStateOf(false) }
                val options = field.options ?: emptyList()
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(label, fontFamily = Vazir, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(16.dp),
                        isError = isError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFa29bfe),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            errorBorderColor = Color.Red
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { formValues[field.realKey] = option; expanded = false })
                        }
                    }
                }
            }
            "checkbox" -> {
                Row(modifier = Modifier.fillMaxWidth().clickable { formValues[field.realKey] = if (value == "true") "false" else "true" }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = value == "true", onCheckedChange = { formValues[field.realKey] = it.toString() }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6C5CE7), uncheckedColor = if (isError) Color.Red else Color.White.copy(alpha = 0.4f)))
                    Text(label, fontFamily = Vazir, fontSize = 14.sp, color = if (isError) Color.Red else Color.White)
                }
            }
            "province" -> {
                var expanded by remember { mutableStateOf(false) }
                val provinces = ProvinceCityData.provinces
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(label, fontFamily = Vazir, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(16.dp),
                        isError = isError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFa29bfe),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            errorBorderColor = Color.Red
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        provinces.forEach { province ->
                            DropdownMenuItem(text = { Text(province) }, onClick = { formValues[field.realKey] = province; expanded = false })
                        }
                    }
                }
            }
            "city" -> {
                val provinceField = formValues.keys.find { it.lowercase().contains("province") } ?: "province"
                val selectedProvince = formValues[provinceField] ?: ""
                val cities = ProvinceCityData.getCities(selectedProvince)
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (cities.isNotEmpty()) expanded = !expanded }) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(label, fontFamily = Vazir, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(16.dp),
                        enabled = cities.isNotEmpty(),
                        isError = isError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFa29bfe),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledBorderColor = Color.White.copy(alpha = 0.1f),
                            errorBorderColor = Color.Red
                        )
                    )
                    if (cities.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            cities.forEach { city ->
                                DropdownMenuItem(text = { Text(city) }, onClick = { formValues[field.realKey] = city; expanded = false })
                            }
                        }
                    }
                }
            }
            "plate", "plate_vehicle", "plate_motorcycle" -> {
                PlateInput(initialValue = value, onValueChange = { formValues[field.realKey] = it }, label = label, plateType = if (canonicalType == "plate_motorcycle") "موتور" else "خودرو")
            }
            "date" -> {
                Text(label, fontFamily = Vazir, fontSize = 13.sp, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { formValues[field.realKey] = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("۱۴۰۳/۰۱/۰۱", color = Color.White.copy(alpha = 0.3f)) },
                    shape = RoundedCornerShape(16.dp),
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = Color.Red
                    )
                )
            }
            "time", "timed" -> {
                Text(label, fontFamily = Vazir, fontSize = 13.sp, color = if (isError) Color.Red else Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { formValues[field.realKey] = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("۱۲:۰۰", color = Color.White.copy(alpha = 0.3f)) },
                    shape = RoundedCornerShape(16.dp),
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = Color.Red
                    )
                )
            }
            else -> {
                Text("نوع فیلد '${field.type}' در فرم اولیه پشتیبانی نمی‌شود.", color = Color.Gray, fontSize = 11.sp, fontFamily = Vazir)
            }
        }
        if (isError && error?.message != null) {
            Text(error.message, color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 8.dp))
        }
    }
}


@Composable
fun ReviewView(
    visibleFields: List<FormField>,
    formValues: Map<String, String>,
    onEdit: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Text("لطفاً اطلاعات زیر را بررسی و در صورت صحت، تأیید کنید:", fontFamily = Vazir, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    visibleFields.forEachIndexed { index, field ->
                        val displayValue = if (field.type == "checkbox") (if (formValues[field.realKey] == "true") "بله" else "خیر") 
                                           else if (field.type == "password") "****"
                                           else formValues[field.realKey] ?: "وارد نشده"
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(field.label + ":", fontFamily = Vazir, fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
                            Text(displayValue, fontFamily = Vazir, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                        if (index < visibleFields.size - 1) HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("ویرایش", fontFamily = Lalezar, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1.5f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
            ) {
                Text("ثبت نهایی درخواست", fontFamily = Lalezar, fontSize = 16.sp)
            }
        }
    }
}
