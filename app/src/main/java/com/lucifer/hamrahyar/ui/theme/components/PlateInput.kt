package com.lucifer.hamrahyar.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.ui.theme.Vazir

@Composable
fun PlateInput(
    initialValue: String,
    onValueChange: (String) -> Unit,
    label: String,
    plateType: String = "خودرو" 
) {
    val type = remember(plateType) {
        when {
            plateType.contains("موتور") -> "motor"
            plateType.contains("آزاد") -> "freezone"
            else -> "car"
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label, 
            fontFamily = Vazir, 
            fontSize = 13.sp, 
            color = Color.White.copy(alpha = 0.7f), 
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        when (type) {
            "motor" -> MotorPlate(initialValue, onValueChange)
            "freezone" -> FreeZonePlate(initialValue, onValueChange)
            else -> CarPlate(initialValue, onValueChange)
        }
    }
}

@Composable
fun CarPlate(value: String, onValueChange: (String) -> Unit) {
    val parts = remember(value) { value.split("-") }
    var part1 by remember { mutableStateOf(parts.getOrNull(0) ?: "") }
    var letter by remember { mutableStateOf(parts.getOrNull(1) ?: "الف") }
    var part2 by remember { mutableStateOf(parts.getOrNull(2) ?: "") }
    var part3 by remember { mutableStateOf(parts.getOrNull(3) ?: "") }

    LaunchedEffect(part1, letter, part2, part3) {
        onValueChange("$part1-$letter-$part2-$part3")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(25.dp)
                .background(Color(0xFF003399), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text("I.R.\nIRAN", color = Color.White, fontSize = 6.sp, lineHeight = 7.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 4.dp))
        }

        PlatePartField(part1, { if (it.length <= 2) part1 = it }, 45.dp, "۲۲")
        LetterPicker(letter) { letter = it }
        PlatePartField(part2, { if (it.length <= 3) part2 = it }, 65.dp, "۳۳۳", Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(Color.Black))
        PlatePartField(part3, { if (it.length <= 2) part3 = it }, 50.dp, "۱۱")
    }
}

@Composable
fun MotorPlate(value: String, onValueChange: (String) -> Unit) {
    val parts = remember(value) { value.split("-") }
    var part1 by remember { mutableStateOf(parts.getOrNull(0) ?: "") }
    var part2 by remember { mutableStateOf(parts.getOrNull(1) ?: "") }

    LaunchedEffect(part1, part2) {
        onValueChange("$part1-$part2")
    }

    Column(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlatePartField(part1, { if (it.length <= 3) part1 = it }, 100.dp, "۱۲۳", Modifier.weight(1f), fontSize = 24.sp)
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
        PlatePartField(part2, { if (it.length <= 5) part2 = it }, 140.dp, "۴۵۶۷۸", Modifier.weight(1.2f), fontSize = 28.sp)
    }
}

@Composable
fun FreeZonePlate(value: String, onValueChange: (String) -> Unit) {
    val parts = remember(value) { value.split("-") }
    var part1 by remember { mutableStateOf(parts.getOrNull(0) ?: "") }
    var city by remember { mutableStateOf(parts.getOrNull(1) ?: "کیش") }

    LaunchedEffect(part1, city) {
        onValueChange("$part1-$city")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(25.dp)
                .background(Color(0xFF003399), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
        )
        PlatePartField(part1, { if (it.length <= 5) part1 = it }, 120.dp, "۱۲۳۴۵", Modifier.weight(1f), fontSize = 24.sp)
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(Color.Black))
        PlatePartField(city, { city = it }, 80.dp, "منطقه", fontSize = 16.sp, keyboardType = KeyboardType.Text)
    }
}

@Composable
fun PlatePartField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp = 40.dp,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.width(width).padding(4.dp),
        textStyle = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black,
            fontFamily = Vazir
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(placeholder, color = Color.Gray.copy(alpha = 0.5f), fontSize = fontSize, textAlign = TextAlign.Center)
                innerTextField()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterPicker(current: String, onSelected: (String) -> Unit) {
    val letters = listOf("الف", "ب", "پ", "ت", "ث", "ج", "د", "ز", "س", "ش", "ص", "ط", "ع", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی", "ژ")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxHeight()
                .width(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(current, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            letters.forEach { letter ->
                DropdownMenuItem(
                    text = { Text(letter) },
                    onClick = {
                        onSelected(letter)
                        expanded = false
                    }
                )
            }
        }
    }
}
