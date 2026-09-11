package com.lucifer.hamrahyar.ui.theme.service

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir

import com.lucifer.hamrahyar.ui.theme.data.local.LocalSixDigitPasswordManager
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SecurityMode {
    VERIFY, SETUP, CHANGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCheckScreen(
    mode: SecurityMode = SecurityMode.VERIFY,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinManager = remember { LocalSixDigitPasswordManager(context) }
    
    var showPinInput by remember { mutableStateOf(false) }
    var showSystemSuggestion by remember { mutableStateOf(false) }
    
    var currentStep by remember { mutableStateOf(if (mode == SecurityMode.CHANGE) "OLD_PIN" else "NEW_PIN") }
    
    var oldPinInput by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

        if (mode == SecurityMode.VERIFY) {
            if (canAuth) {
                showBiometricPrompt(context, onSuccess, { showPinInput = true })
            } else {
                if (!pinManager.isPinSet()) onSuccess() else showPinInput = true
            }
        } else if (mode == SecurityMode.SETUP) {
            if (!canAuth) {
                showSystemSuggestion = true
            } else {
                // System security is already on, app pin might not be needed but user clicked it
                showPinInput = true
            }
        } else {
            showPinInput = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63))
                    )
                )
            }
    ) {
        if (showSystemSuggestion) {
            SystemSecuritySuggestion(
                onContinue = { 
                    showSystemSuggestion = false
                    showPinInput = true
                },
                onOpenSettings = {
                    try {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا در باز کردن تنظیمات", Toast.LENGTH_SHORT).show()
                    }
                },
                onBack = onBack
            )
        } else if (showPinInput) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        if (mode == SecurityMode.VERIFY) "تأیید امنیتی" else "تنظیم رمز عبور",
                        fontFamily = Lalezar, fontSize = 22.sp, color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Icon(
                    Icons.Rounded.Lock,
                    null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFFa29bfe)
                )

                Spacer(modifier = Modifier.height(20.dp))

                val promptText = when (currentStep) {
                    "OLD_PIN" -> "رمز عبور فعلی خود را وارد کنید"
                    "NEW_PIN" -> if (mode == SecurityMode.VERIFY) "رمز عبور ۶ رقمی را وارد کنید" else "رمز عبور ۶ رقمی جدید را وارد کنید"
                    "CONFIRM_PIN" -> "رمز عبور جدید را دوباره وارد کنید"
                    else -> ""
                }

                Text(
                    promptText,
                    fontFamily = Vazir,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                val currentInputValue = when (currentStep) {
                    "OLD_PIN" -> oldPinInput
                    "NEW_PIN" -> pin
                    "CONFIRM_PIN" -> confirmPin
                    else -> ""
                }

                OutlinedTextField(
                    value = currentInputValue,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            when (currentStep) {
                                "OLD_PIN" -> oldPinInput = it
                                "NEW_PIN" -> pin = it
                                "CONFIRM_PIN" -> confirmPin = it
                            }
                        }
                    },
                    modifier = Modifier.width(200.dp),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = Vazir,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFa29bfe),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                if (error != null) {
                    Text(
                        error!!,
                        color = Color(0xFFFF7675),
                        fontFamily = Vazir,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        scope.launch {
                            when (currentStep) {
                                "OLD_PIN" -> {
                                    if (pinManager.verifyPin(oldPinInput)) {
                                        currentStep = "NEW_PIN"
                                        error = null
                                    } else {
                                        error = "رمز عبور فعلی اشتباه است"
                                        oldPinInput = ""
                                    }
                                }
                                "NEW_PIN" -> {
                                    if (pin.length == 6) {
                                        if (mode == SecurityMode.VERIFY) {
                                            if (pinManager.verifyPin(pin)) onSuccess() else {
                                                error = "رمز عبور اشتباه است"
                                                pin = ""
                                            }
                                        } else {
                                            currentStep = "CONFIRM_PIN"
                                            error = null
                                        }
                                    } else {
                                        error = "رمز باید ۶ رقم باشد"
                                    }
                                }
                                "CONFIRM_PIN" -> {
                                    if (pin == confirmPin) {
                                        pinManager.savePin(pin)
                                        Toast.makeText(context, "رمز عبور با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        error = "رمزها مطابقت ندارند"
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (currentStep == "CONFIRM_PIN" || (mode == SecurityMode.VERIFY && currentStep == "NEW_PIN")) "تأیید" else "ادامه",
                        fontFamily = Lalezar,
                        fontSize = 18.sp
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun SystemSecuritySuggestion(
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Security, null, modifier = Modifier.size(80.dp), tint = Color(0xFFa29bfe))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "پیشنهاد امنیتی",
            fontFamily = Lalezar, fontSize = 24.sp, color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "برای امنیت بیشتر، پیشنهاد می‌شود یکی از روش‌های امنیتی اندروید (اثر انگشت، پین یا الگو) را در تنظیمات گوشی خود فعال کنید.",
            fontFamily = Vazir, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center, lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("باز کردن تنظیمات گوشی", fontFamily = Lalezar, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("ادامه با رمز ۶ رقمی اختصاصی", fontFamily = Lalezar, fontSize = 16.sp, color = Color.White)
        }
        
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("انصراف", fontFamily = Vazir, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

private fun showBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val activity = context as? FragmentActivity ?: return onError()
    val executor = ContextCompat.getMainExecutor(activity)
    
    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                onError()
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
        }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("تأیید هویت")
        .setSubtitle("برای ثبت سفارش، هویت خود را تأیید کنید")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}
