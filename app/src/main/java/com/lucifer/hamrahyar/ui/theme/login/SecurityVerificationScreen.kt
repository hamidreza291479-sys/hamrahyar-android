package com.lucifer.hamrahyar.ui.theme.login

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lucifer.hamrahyar.R
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityVerificationScreen(
    savedPin: String?,
    isLoading: Boolean = false,
    onSecurityPassed: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showPinUi by remember { mutableStateOf(false) }
    var isCheckingDeviceSecurity by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "security3d")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floating"
    )

    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val authStatus = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        val activity = context.findFragmentActivity()
        if (authStatus == BiometricManager.BIOMETRIC_SUCCESS && activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSecurityPassed("DEVICE_SECURITY_PASSED")
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                         if (savedPin != null) showPinUi = true
                    }
                }
            })
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("تایید هویت امن")
                .setSubtitle("لطفاً برای ادامه، هویت خود را تایید کنید")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
            biometricPrompt.authenticate(promptInfo)
            isCheckingDeviceSecurity = false
        } else {
            showPinUi = true
            isCheckingDeviceSecurity = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0F0C29), Color(0xFF24243E))))
                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(Color(0xFF00B894).copy(alpha = 0.15f), Color.Transparent)),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = 500f
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("امنیت دسترسی", fontFamily = Lalezar, fontSize = 22.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.weight(0.2f))

            if (isCheckingDeviceSecurity) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (showPinUi) {
                // Logo at top of security
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp).graphicsLayer { translationY = floatingOffset }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().graphicsLayer { rotationX = -floatingOffset / 2 },
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (savedPin == null) "تنظیم رمز عبور" else "ورود رمز عبور",
                            fontFamily = Lalezar, fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                            placeholder = { Text("رمز ۶ رقمی", fontFamily = Vazir, color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(alpha = 0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00B894),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            singleLine = true
                        )

                        if (savedPin == null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                                placeholder = { Text("تکرار رمز عبور", fontFamily = Vazir, color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00B894),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        if (error != null) {
                            Text(error!!, color = Color(0xFFFF7675), fontFamily = Vazir, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (savedPin == null) {
                                    if (pin.length == 6 && pin == confirmPin) onSecurityPassed(pin)
                                    else error = if (pin != confirmPin) "رمزها مطابقت ندارند" else "رمز باید ۶ رقم باشد"
                                } else {
                                    if (pin == savedPin) onSecurityPassed(pin)
                                    else error = "رمز اشتباه است"
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer { shadowElevation = 10f; shape = RoundedCornerShape(16.dp) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("ثبت درخواست...", fontFamily = Lalezar, fontSize = 18.sp, color = Color.White)
                            } else {
                                Text("تایید و ادامه", fontFamily = Lalezar, fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.7f))
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}
