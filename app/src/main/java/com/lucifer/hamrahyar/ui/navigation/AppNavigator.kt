package com.lucifer.hamrahyar.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucifer.hamrahyar.ui.home.model.ServiceCategory
import com.lucifer.hamrahyar.ui.home.model.ServiceItem
import com.lucifer.hamrahyar.ui.theme.service.DynamicFormScreen
import com.lucifer.hamrahyar.ui.theme.domain.model.ActiveService
import com.lucifer.hamrahyar.ui.theme.data.model.FormFieldError
import com.lucifer.hamrahyar.ui.theme.domain.model.PriorityLevel
import com.lucifer.hamrahyar.ui.theme.domain.model.ServiceRequest
import com.lucifer.hamrahyar.ui.theme.domain.model.ServiceStatus
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.domain.usecase.CreateOrderUseCase
import com.lucifer.hamrahyar.ui.theme.login.MobileVerificationScreen
import com.lucifer.hamrahyar.ui.theme.data.local.LocalSixDigitPasswordManager
import com.lucifer.hamrahyar.ui.settings.GenericContentScreen
import com.lucifer.hamrahyar.ui.settings.NotificationsScreen
import com.lucifer.hamrahyar.ui.theme.service.SecurityMode
import com.lucifer.hamrahyar.ui.theme.service.ConfirmInfoScreen
import com.lucifer.hamrahyar.ui.theme.service.SecurityCheckScreen
import com.lucifer.hamrahyar.ui.theme.service.PrioritySelectionScreen
import com.lucifer.hamrahyar.ui.theme.service.ChatScreen
import com.lucifer.hamrahyar.ui.theme.service.OrderRegistrationState
import com.lucifer.hamrahyar.ui.theme.service.OrderViewModel
import com.lucifer.hamrahyar.ui.splash.SplashScreen
import com.lucifer.hamrahyar.ui.support.SupportScreen
import com.lucifer.hamrahyar.ui.theme.support.SupportViewModel
import com.lucifer.hamrahyar.ui.settings.SettingsScreen
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import com.lucifer.hamrahyar.ui.theme.data.local.SecureServiceManager
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log

enum class Screen {
    SPLASH, HOME, PERSONAL_INFO, FORM, PRIORITY_SELECTION, CONFIRM_INFO, SECURITY_CHECK, CHAT, SUPPORT, MOBILE_VERIFICATION, SETTINGS, NOTIFICATIONS, ABOUT, TERMS
}

@Composable
fun AppNavigator(repository: OnlineServiceRepository) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val secureManager = remember { SecureServiceManager(context) }
    val pinManager = remember { LocalSixDigitPasswordManager(context) }
    val scope = rememberCoroutineScope()
    val clientId = remember { java.util.UUID.randomUUID().toString() }

    var screenStack by remember { mutableStateOf(listOf(Screen.SPLASH)) }
    var isSplashFinished by remember { mutableStateOf(false) }

    // Heartbeat for Presence - Only start after Splash to avoid blocking startup
    LaunchedEffect(isSplashFinished) {
        if (isSplashFinished) {
            while(true) {
                repository.touchPresence(clientId)
                kotlinx.coroutines.delay(30000) // 30 seconds
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch { repository.clearPresence(clientId) }
        }
    }

    // State for Categories and Services
    var categories by remember { mutableStateOf<List<ServiceCategory>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedSubServiceId by remember { mutableStateOf<String?>(null) }
    var selectedPriority by remember { mutableStateOf<PriorityLevel?>(null) }
    var pendingFormData by remember { mutableStateOf<String?>(null) }

    // Profile State
    var userName by remember { mutableStateOf<String?>(null) }
    var userMobile by remember { mutableStateOf<String?>(null) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var savedPin by remember { mutableStateOf<String?>(null) }
    var securityMode by remember { mutableStateOf(SecurityMode.VERIFY) }

    // Active Service State
    var activeRequest by remember { mutableStateOf<ServiceRequest?>(null) }
    val activeServicesList = remember { mutableStateListOf<ActiveService>() }
    
    // UI Logic states
    var isDataLoading by remember { mutableStateOf(false) }
    var formErrors by remember { mutableStateOf<List<FormFieldError>?>(null) }

    val createOrderUseCase = remember { CreateOrderUseCase(repository) }
    val orderViewModel: OrderViewModel = viewModel(
        factory = com.lucifer.hamrahyar.ui.theme.service.OrderViewModelFactory(createOrderUseCase, repository)
    )
    val registrationState by orderViewModel.registrationState.collectAsState()

    fun navigateTo(screen: Screen) {
        if (screenStack.lastOrNull() != screen) {
            screenStack = screenStack + screen
        }
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack = screenStack.dropLast(1)
        }
    }

    fun popToHome() {
        screenStack = listOf(Screen.HOME)
    }

    BackHandler(enabled = isSplashFinished) { navigateBack() }

    // Initial check for active order
    LaunchedEffect(Unit) {
        preferenceManager.guestKey.first()?.let { key ->
            orderViewModel.checkForActiveOrder(key)
        }
    }

    // Handle Order Registration States
    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is OrderRegistrationState.Success -> {
                val restored = state.activeService
                activeRequest = ServiceRequest(
                    id = restored.orderId, trackingNumber = restored.trackingNumber ?: restored.accessId.takeLast(8),
                    categoryId = selectedCategoryId ?: "", subServiceId = restored.serviceId, customerMobile = userMobile ?: "",
                    status = ServiceStatus.fromValue(restored.status), priority = PriorityLevel.fromValue(restored.priority),
                    createdAt = restored.createdAt, lastUpdate = restored.lastValidatedAt,
                    conversationId = restored.conversationId, serviceName = restored.serviceName, accessToken = restored.accessToken,
                    profileId = restored.profileId, adminName = restored.adminName, adminAvatar = restored.adminAvatar, adminRole = restored.adminRole,
                    customerFullName = restored.customerFullName ?: userName, customerEmail = restored.customerEmail ?: userEmail,
                    result = restored.result, deleteAfter = restored.deleteAfter,
                    serviceAmount = restored.serviceAmount, speedAmount = restored.speedAmount,
                    discountAmount = restored.discountAmount, totalAmount = restored.totalAmount
                )
                if (!activeServicesList.any { it.orderId == restored.orderId }) {
                    activeServicesList.add(restored)
                }

                if (restored.status == "pending" || restored.status == "submitted") {
                    Toast.makeText(context, "درخواست شما با موفقیت ثبت شده و منتظر تایید کارشناسان همراه یار است", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "سفارش شما با موفقیت ثبت شد. شماره پیگیری: ${restored.trackingNumber ?: restored.accessId.takeLast(8)}", Toast.LENGTH_LONG).show()
                }

                pendingFormData = null
                formErrors = null
                isDataLoading = false
                orderViewModel.resetState()
                popToHome()
                navigateTo(Screen.CHAT)
            }
            is OrderRegistrationState.ActiveOrderExists -> {
                val existing = state.activeService
                activeRequest = ServiceRequest(
                    id = existing.orderId, trackingNumber = existing.trackingNumber ?: existing.accessId.takeLast(8),
                    categoryId = selectedCategoryId ?: "", subServiceId = existing.serviceId, customerMobile = userMobile ?: "",
                    status = ServiceStatus.fromValue(existing.status), priority = PriorityLevel.fromValue(existing.priority),
                    createdAt = existing.createdAt, lastUpdate = existing.lastValidatedAt,
                    conversationId = existing.conversationId, serviceName = existing.serviceName, accessToken = existing.accessToken,
                    profileId = existing.profileId, adminName = existing.adminName, adminAvatar = existing.adminAvatar, adminRole = existing.adminRole,
                    customerFullName = existing.customerFullName ?: userName, customerEmail = existing.customerEmail ?: userEmail,
                    result = existing.result, deleteAfter = existing.deleteAfter,
                    serviceAmount = existing.serviceAmount, speedAmount = existing.speedAmount,
                    discountAmount = existing.discountAmount, totalAmount = existing.totalAmount
                )
                if (!activeServicesList.any { it.orderId == existing.orderId }) {
                    activeServicesList.add(existing)
                }
                pendingFormData = null
                isDataLoading = false
                orderViewModel.resetState()
                if (screenStack.last() != Screen.SPLASH) {
                    Toast.makeText(context, "شما در حال حاضر یک سفارش فعال دارید.", Toast.LENGTH_LONG).show()
                }
                popToHome()
                navigateTo(Screen.CHAT)
            }
            is OrderRegistrationState.ValidationError -> {
                formErrors = state.errors
                isDataLoading = false
                Toast.makeText(context, "اطلاعات فرم ناقص است. لطفا فیلدهای مشخص شده را تکمیل کنید.", Toast.LENGTH_LONG).show()
                orderViewModel.resetState()
            }
            is OrderRegistrationState.Error -> {
                isDataLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                orderViewModel.resetState()
            }
            OrderRegistrationState.Idle -> {}
            OrderRegistrationState.Submitting -> { isDataLoading = true }
        }
    }

    // Startup Data Loading
    LaunchedEffect(Unit) {
        isDataLoading = true
        try {
            userName = preferenceManager.customerName.first()
            userMobile = preferenceManager.customerMobile.first()
            userEmail = preferenceManager.customerEmail.first()
            savedPin = pinManager.getPin()
            
            if (userMobile != null && userName != null) {
                repository.recordLogin()
            }
            
            categories = repository.getCategories()

            // Forced recovery via guest key on startup
            preferenceManager.guestKey.first()?.let { key ->
                repository.getLatestOrderEvent(key).onSuccess { event ->
                    val recovered = event?.order
                    if (recovered != null) {
                        // Only auto-navigate/show if it's NOT cancelled
                        if (recovered.status != "cancelled") {
                            activeRequest = ServiceRequest(
                                id = recovered.orderId, trackingNumber = recovered.trackingNumber ?: recovered.accessId.takeLast(8),
                                categoryId = "", subServiceId = recovered.serviceId, customerMobile = userMobile ?: "",
                                status = ServiceStatus.fromValue(recovered.status), priority = PriorityLevel.fromValue(recovered.priority),
                                createdAt = recovered.createdAt, lastUpdate = recovered.lastValidatedAt,
                                conversationId = recovered.conversationId, serviceName = recovered.serviceName, accessToken = recovered.accessToken,
                                profileId = recovered.profileId, adminName = recovered.adminName, adminAvatar = recovered.adminAvatar, adminRole = recovered.adminRole,
                                customerFullName = recovered.customerFullName ?: userName, customerEmail = recovered.customerEmail ?: userEmail,
                                result = recovered.result ?: event.result, deleteAfter = recovered.deleteAfter,
                                serviceAmount = recovered.serviceAmount, speedAmount = recovered.speedAmount,
                                discountAmount = recovered.discountAmount, totalAmount = recovered.totalAmount
                            )
                            if (!activeServicesList.any { it.orderId == recovered.orderId }) {
                                activeServicesList.add(recovered)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { 
            Log.e("AppNavigator", "Startup error: ${e.message}") 
        } finally {
            isDataLoading = false
        }

        // Observation Jobs
        launch {
            repository.observeOrderUpdates().collect { updated ->
                val index = activeServicesList.indexOfFirst { it.orderId == updated.orderId }
                if (index != -1) {
                    activeServicesList[index] = updated
                } else {
                    activeServicesList.add(updated)
                }
                
                if (activeRequest?.id == updated.orderId) {
                    activeRequest = activeRequest?.copy(
                        status = ServiceStatus.fromValue(updated.status),
                        lastUpdate = updated.lastValidatedAt,
                        conversationId = updated.conversationId,
                        adminName = updated.adminName,
                        adminAvatar = updated.adminAvatar,
                        adminRole = updated.adminRole,
                        customerFullName = updated.customerFullName ?: activeRequest?.customerFullName,
                        customerEmail = updated.customerEmail ?: activeRequest?.customerEmail,
                        serviceName = updated.serviceName,
                        trackingNumber = updated.trackingNumber ?: activeRequest?.trackingNumber ?: "",
                        result = updated.result,
                        deleteAfter = updated.deleteAfter,
                        serviceAmount = updated.serviceAmount,
                        speedAmount = updated.speedAmount,
                        discountAmount = updated.discountAmount,
                        totalAmount = updated.totalAmount
                    )
                }
            }
        }

        launch {
            repository.observeRealtimeStatus().collect { status ->
                if (status == "CONNECTED") {
                    preferenceManager.guestKey.first()?.let { key ->
                        repository.getMyActiveOrder(key).onSuccess { recovered ->
                            if (recovered != null) {
                                val updatedRequest = ServiceRequest(
                                    id = recovered.orderId, trackingNumber = recovered.trackingNumber ?: recovered.accessId.takeLast(8),
                                    categoryId = "", subServiceId = recovered.serviceId, customerMobile = userMobile ?: "",
                                    status = ServiceStatus.fromValue(recovered.status), priority = PriorityLevel.fromValue(recovered.priority),
                                    createdAt = recovered.createdAt, lastUpdate = recovered.lastValidatedAt,
                                    conversationId = recovered.conversationId, serviceName = recovered.serviceName, accessToken = recovered.accessToken,
                                    profileId = recovered.profileId, adminName = recovered.adminName, adminAvatar = recovered.adminAvatar, adminRole = recovered.adminRole,
                                    customerFullName = recovered.customerFullName ?: userName, customerEmail = recovered.customerEmail ?: userEmail,
                                    result = recovered.result, deleteAfter = recovered.deleteAfter,
                                    serviceAmount = recovered.serviceAmount, speedAmount = recovered.speedAmount,
                                    discountAmount = recovered.discountAmount, totalAmount = recovered.totalAmount
                                )
                                activeRequest = updatedRequest
                                
                                val index = activeServicesList.indexOfFirst { it.orderId == recovered.orderId }
                                if (index != -1) activeServicesList[index] = recovered
                                else activeServicesList.add(recovered)
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screenStack.last(),
            transitionSpec = {
                // Smooth transition for startup
                if (targetState != Screen.SPLASH && initialState == Screen.SPLASH) {
                    fadeIn(tween(600)).togetherWith(fadeOut(tween(600)))
                } else if (screenStack.size > 1) { // Forward navigation
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else { // Backward navigation or same level
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            }, label = "navigation"
        ) { currentScreen ->
            when (currentScreen) {
                Screen.SPLASH -> SplashScreen(onFinished = { 
                    isSplashFinished = true
                    // Navigate immediately when splash animation is done
                    if (activeRequest != null) {
                        screenStack = listOf(Screen.HOME, Screen.CHAT)
                    } else {
                        popToHome()
                    }
                })
                
                Screen.HOME -> com.lucifer.hamrahyar.ui.home.HomeScreen(
                    activeRequest = activeRequest,
                    categories = categories,
                    isLoading = isDataLoading,
                    onActiveRequestClick = { navigateTo(Screen.CHAT) },
                    onCategoryClick = { id: String -> selectedCategoryId = id },
                    onSubServiceClick = { id: String ->
                        if (activeRequest != null) {
                            navigateTo(Screen.CHAT)
                        } else {
                            selectedSubServiceId = id
                            navigateTo(Screen.PERSONAL_INFO)
                        }
                    },
                    onSettingsClick = { navigateTo(Screen.SETTINGS) },
                    onExit = { (context as? android.app.Activity)?.finishAffinity() }
                )

                Screen.PERSONAL_INFO -> MobileVerificationScreen(
                    title = "اطلاعات فردی",
                    initialPhone = userMobile, initialName = userName, initialEmail = userEmail,
                    onProfileConfirmed = { phone, name, email ->
                        userMobile = phone; userName = name; userEmail = email
                        scope.launch { 
                            preferenceManager.saveMobile(phone); preferenceManager.saveName(name); preferenceManager.saveEmail(email)
                            repository.recordLogin()
                        }
                        navigateTo(Screen.FORM)
                    },
                    onBack = { navigateBack() }
                )

                Screen.FORM -> DynamicFormScreen(
                    serviceId = selectedSubServiceId ?: "",
                    repository = repository,
                    initialData = null,
                    errors = formErrors,
                    onFormSubmit = { formData: String ->
                        pendingFormData = formData
                        navigateTo(Screen.PRIORITY_SELECTION)
                    },
                    onBack = { navigateBack() }
                )

                Screen.PRIORITY_SELECTION -> PrioritySelectionScreen(
                    repository = repository,
                    onPrioritySelected = { priority ->
                        selectedPriority = priority
                        navigateTo(Screen.CONFIRM_INFO)
                    },
                    onBack = { navigateBack() }
                )

                Screen.CONFIRM_INFO -> {
                    val serviceName = categories.flatMap { it.services }.find { it.id == selectedSubServiceId }?.title ?: ""
                    ConfirmInfoScreen(
                        serviceName = serviceName,
                        userName = userName ?: "",
                        userMobile = userMobile ?: "",
                        userEmail = userEmail,
                        formData = pendingFormData,
                        priority = selectedPriority ?: PriorityLevel.NORMAL,
                        onConfirm = { 
                            securityMode = SecurityMode.VERIFY
                            navigateTo(Screen.SECURITY_CHECK) 
                        },
                        onEdit = { 
                            // Go back to form, or personal info? The requirement says "back to corresponding form"
                            // For simplicity, let's go back to FORM
                            navigateBack(); navigateBack() // Pops CONFIRM and PRIORITY, goes to FORM
                        },
                        onBack = { navigateBack() }
                    )
                }

                Screen.SECURITY_CHECK -> SecurityCheckScreen(
                    mode = securityMode,
                    onSuccess = {
                        savedPin = pinManager.getPin()
                        if (securityMode == SecurityMode.VERIFY) {
                            orderViewModel.submitOrder(selectedSubServiceId!!, selectedPriority!!, pendingFormData)
                        } else {
                            // After setup/change in settings, go back
                            navigateBack()
                        }
                    },
                    onBack = { navigateBack() }
                )

                Screen.CHAT -> {
                    activeRequest?.let { request ->
                        ChatScreen(
                            request = request,
                            repository = repository,
                            categories = categories,
                            onActiveRequestUpdate = { updatedRequest ->
                                activeRequest = updatedRequest
                            },
                            onBack = { popToHome() }
                        )
                    } ?: run {
                        // Fallback if activeRequest becomes null while on CHAT screen
                        LaunchedEffect(Unit) { popToHome() }
                    }
                }

                Screen.SUPPORT -> SupportScreen(
                    repository = repository,
                    onBack = { navigateBack() }
                )

                Screen.SETTINGS -> SettingsScreen(
                    onBack = { navigateBack() },
                    onSupportClick = { navigateTo(Screen.SUPPORT) },
                    onSecurityClick = { isSetup ->
                        securityMode = if (!pinManager.isPinSet()) SecurityMode.SETUP else SecurityMode.CHANGE
                        navigateTo(Screen.SECURITY_CHECK)
                    },
                    onNotificationsClick = { navigateTo(Screen.NOTIFICATIONS) },
                    onAboutClick = { navigateTo(Screen.ABOUT) },
                    onTermsClick = { navigateTo(Screen.TERMS) },
                    isPinSet = pinManager.isPinSet()
                )

                Screen.NOTIFICATIONS -> NotificationsScreen(
                    repository = repository,
                    onBack = { navigateBack() }
                )

                Screen.ABOUT -> GenericContentScreen(
                    contentKey = "about_content",
                    repository = repository,
                    onBack = { navigateBack() }
                )

                Screen.TERMS -> GenericContentScreen(
                    contentKey = "terms_content",
                    repository = repository,
                    onBack = { navigateBack() }
                )

                Screen.MOBILE_VERIFICATION -> MobileVerificationScreen(
                    initialPhone = userMobile, initialName = userName, initialEmail = userEmail,
                    onProfileConfirmed = { phone, name, email ->
                        userMobile = phone; userName = name; userEmail = email
                        scope.launch { 
                            preferenceManager.saveMobile(phone); preferenceManager.saveName(name); preferenceManager.saveEmail(email)
                            repository.recordLogin()
                        }
                        popToHome()
                    },
                    onBack = { navigateBack() }
                )
            }
        }

        if (isDataLoading && screenStack.last() != Screen.SPLASH) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
