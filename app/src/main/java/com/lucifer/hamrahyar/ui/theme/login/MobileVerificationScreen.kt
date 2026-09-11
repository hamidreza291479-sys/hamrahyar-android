package com.lucifer.hamrahyar.ui.theme.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.R
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileVerificationScreen(
    title: String = "احراز هویت کاربر",
    initialPhone: String? = null,
    initialName: String? = null,
    initialEmail: String? = null,
    onProfileConfirmed: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone ?: "") }
    var fullName by remember { mutableStateOf(initialName ?: "") }
    var email by remember { mutableStateOf(initialEmail ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "login3d")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63))
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6C5CE7).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = 600f
                    )
                )
            }
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, fontFamily = Lalezar, fontSize = 22.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Fixed Logo Section ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { translationY = floatingOffset }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(130.dp)
                ) {
                    // Glow background
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF6C5CE7).copy(alpha = 0.4f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Hamrahyar Logo",
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "همراه یار",
                    fontFamily = Lalezar,
                    fontSize = 32.sp,
                    color = Color.White
                )
                Text(
                    text = "دستیار هوشمند خدمات آنلاین شما",
                    fontFamily = Vazir,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth().graphicsLayer { rotationX = floatingOffset / 4 },
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("تکمیل اطلاعات", fontFamily = Lalezar, fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = { Text("نام و نام خانوادگی", fontFamily = Vazir, color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFa29bfe),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 11 && it.all { char -> char.isDigit() }) phone = it },
                        placeholder = { Text("شماره همراه (مثلاً 0912...)", fontFamily = Vazir, color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null, tint = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFa29bfe),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("آدرس ایمیل", fontFamily = Vazir, color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFa29bfe),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        singleLine = true
                    )

                    if (error != null) {
                        Text(error!!, color = Color(0xFFFF7675), fontFamily = Vazir, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            // فقط بررسی قواره کلی ایمیل (داشتن @ و نقطه)
                            val emailPattern = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$".toRegex()
                            val hasEmailFormat = email.isBlank() || emailPattern.matches(email)
                            
                            if (fullName.isNotBlank() && phone.length == 11 && phone.startsWith("09") && hasEmailFormat) {
                                onProfileConfirmed(phone, fullName, email)
                            } else {
                                error = when {
                                    fullName.isBlank() -> "وارد کردن نام الزامی است"
                                    phone.isBlank() -> "وارد کردن شماره همراه الزامی است"
                                    phone.length != 11 || !phone.startsWith("09") -> "شماره همراه باید ۱۱ رقم و با ۰۹ شروع شود"
                                    !hasEmailFormat -> "قالب ایمیل صحیح نیست (مثال: user@mail.com)"
                                    else -> "لطفاً موارد الزامی را تکمیل کنید"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer { shadowElevation = 10f; shape = RoundedCornerShape(16.dp) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("تأیید و ادامه", fontFamily = Lalezar, fontSize = 18.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
