package com.lucifer.hamrahyar.ui.theme.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.home.components.PlateInput
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun FormRequestCard(
    request: FormRequestDto,
    onSubmit: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = request.prompt,
                fontFamily = Lalezar,
                fontSize = 16.sp,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (request.type) {
                "numeric" -> NumericForm(onSubmit)
                "vehicle_plate" -> PlateForm("car", onSubmit)
                "motorcycle_plate" -> PlateForm("motor", onSubmit)
                "text" -> TextForm(onSubmit)
                else -> {
                    Text("این نوع فرم هنوز پشتیبانی نمی‌شود: ${request.type}", color = Color.Gray, fontSize = 12.sp)
                }
            }
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
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Button(
            onClick = { onSubmit(buildJsonObject { put("value", value) }.toString()) },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End),
            enabled = value.isNotBlank()
        ) {
            Text("ارسال")
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
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Button(
            onClick = { onSubmit(buildJsonObject { put("value", value) }.toString()) },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End),
            enabled = value.isNotBlank()
        ) {
            Text("ارسال")
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
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End),
            enabled = plateValue.isNotBlank()
        ) {
            Text("ارسال پلاک")
        }
    }
}
