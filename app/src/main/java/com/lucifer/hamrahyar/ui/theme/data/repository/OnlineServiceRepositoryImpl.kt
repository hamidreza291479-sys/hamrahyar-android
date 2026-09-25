package com.lucifer.hamrahyar.ui.theme.data.repository

import android.content.Context
import android.util.Log
import com.lucifer.hamrahyar.BuildConfig
import com.lucifer.hamrahyar.ui.home.model.ServiceCategory
import com.lucifer.hamrahyar.ui.home.model.ServiceItem
import com.lucifer.hamrahyar.ui.theme.data.local.SecureServiceManager
import com.lucifer.hamrahyar.ui.theme.data.model.*
import com.lucifer.hamrahyar.ui.theme.data.remote.SupabaseClient
import com.lucifer.hamrahyar.ui.theme.domain.model.*
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.utils.IconMapper
import com.lucifer.hamrahyar.ui.theme.utils.PreferenceManager
import com.lucifer.hamrahyar.ui.theme.utils.StyleMapper
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.datetime.*

class OnlineServiceRepositoryImpl(context: Context) : OnlineServiceRepository {

    private val client = SupabaseClient.client
    private val secureManager = SecureServiceManager(context)
    private val preferenceManager = PreferenceManager(context)
    private val TAG = "ServiceRepo"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true; encodeDefaults = true }
    
    private val cachedServices = mutableMapOf<String, ActiveServiceDto>()
    private var cachedCategories: List<ServiceCategory>? = null
    private var lastCacheTime: Long = 0
    private val CACHE_TTL = 10 * 60 * 1000 // 10 minutes

    private val presenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isPresenceJobRunning = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastPresenceTouchTime = 0L

    private suspend fun getOrCreateGuestKey(): String {
        val existing = preferenceManager.guestKey.first()
        if (!existing.isNullOrBlank()) return existing
        val newKey = java.util.UUID.randomUUID().toString()
        preferenceManager.saveGuestKey(newKey)
        return newKey
    }

    private val authMutex = Mutex()

    private suspend fun ensureAuthSession(): Boolean = withContext(Dispatchers.IO) {
        authMutex.withLock {
            try {
                // 1. Wait for Auth initialization (restoration from PreferenceSessionManager)
                client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
                
                var session = client.auth.currentSessionOrNull()
                if (session != null) {
                    val isExpired = session.expiresAt < kotlinx.datetime.Clock.System.now()
                    if (isExpired) {
                        Log.d(TAG, "Auth: Session expired, attempting refresh...")
                        try {
                            client.auth.refreshCurrentSession()
                            Log.d(TAG, "Auth: Session refreshed successfully.")
                            session = client.auth.currentSessionOrNull()
                        } catch (e: Exception) {
                            Log.e(TAG, "Auth: Session refresh failed: ${e.message}")
                            // DO NOT call signInAnonymously() here if we already have a user, 
                            // as it would create a NEW user and break ownership logic.
                            val currentSession = client.auth.currentSessionOrNull() ?: session
                            if (currentSession?.accessToken?.isNotBlank() == true) {
                                Log.w(TAG, "Auth: Continuing with existing session despite refresh error.")
                                return@withLock true
                            }
                            return@withLock false 
                        }
                    }

                    // Promote user if anonymous but profile data is present locally
                    if (session != null) {
                        val isAnonUser = session.user?.email == null && session.user?.phone == null
                        if (isAnonUser) {
                            val localPhone = preferenceManager.customerMobile.first() ?: ""
                            val localEmail = preferenceManager.customerEmail.first() ?: ""
                            val localName = preferenceManager.customerName.first() ?: ""
                            if (localPhone.isNotBlank() || localEmail.isNotBlank()) {
                                Log.d(TAG, "Auth: Promoting anonymous user with local info...")
                                try {
                                    client.auth.updateUser {
                                        if (localPhone.isNotBlank()) phone = localPhone
                                        if (localEmail.isNotBlank()) email = localEmail
                                        data = buildJsonObject {
                                            put("full_name", localName)
                                        }
                                    }
                                    Log.d(TAG, "Auth: Anonymous user successfully promoted.")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Auth: Failed to promote anonymous user: ${e.message}")
                                }
                            }
                        }
                    }
                    return@withLock client.auth.currentSessionOrNull() != null
                }
                
                // 2. No session found
                val identityExisted = SupabaseClient.sessionManager.hasInitializedAuth
                if (identityExisted) {
                    Log.w(TAG, "Auth: Session lost but identity previously existed. Preventing new user recreation.")
                    return@withLock false
                }

                Log.d(TAG, "Auth: First-time setup. Creating new anonymous user...")
                client.auth.signInAnonymously()
                val newSession = client.auth.currentSessionOrNull()
                return@withLock newSession != null
                
            } catch (e: Exception) {
                Log.e(TAG, "Auth: critical failure")
                return@withLock false
            }
        }
    }

    override suspend fun getCategories(): List<ServiceCategory> = coroutineScope {
        val now = System.currentTimeMillis()
        if (cachedCategories != null && (now - lastCacheTime < CACHE_TTL)) {
            launch { refreshCategories() } // Background sync
            return@coroutineScope cachedCategories!!
        }
        refreshCategories()
    }

    private suspend fun refreshCategories(): List<ServiceCategory> = coroutineScope {
        val startTime = System.currentTimeMillis()
        try {
            val authOk = ensureAuthSession()
            Log.d(TAG, "refreshCategories: authOk=$authOk")

            val servicesDeferred = async {
                try {
                    client.postgrest.rpc("get_active_services_fast", buildJsonObject {}).decodeAs<List<ActiveServiceDto>>()
                } catch (e: Exception) {
                    Log.e(TAG, "RPC get_active_services_fast failed. Falling back to table fetch.")
                    try {
                        client.from("services").select {
                            filter { eq("is_active", true) }
                        }.decodeAs<List<ActiveServiceDto>>()
                    } catch (e2: Exception) {
                        Log.e(TAG, "Table fetch also failed.")
                        emptyList<ActiveServiceDto>()
                    }
                }
            }
            
            val categoriesDeferred = async {
                client.from("categories").select {
                    filter { eq("is_active", true) }
                    order("sort_order", Order.ASCENDING)
                }.decodeAs<List<CategoryDto>>()
            }

            val services = try { 
                servicesDeferred.await() 
            } catch (e: Exception) { 
                Log.e(TAG, "Services fetch failed")
                emptyList() 
            }
            
            val categories = try { 
                categoriesDeferred.await() 
            } catch (e: Exception) { 
                Log.e(TAG, "Categories fetch failed")
                emptyList() 
            }

            Log.d(TAG, "Fetched ${categories.size} categories and ${services.size} services in ${System.currentTimeMillis() - startTime}ms")

            if (categories.isEmpty()) {
                Log.w(TAG, "Categories fetch returned empty. Trying to use cache.")
                return@coroutineScope cachedCategories ?: throw Exception("خطا در دریافت لیست خدمات. لطفا اتصال خود را بررسی کنید.")
            }
            
            if (services.isEmpty()) {
                Log.w(TAG, "Services fetch returned empty.")
            }

            services.forEach { cachedServices[it.id] = it }

            val mapped = categories.map { catDto ->
                val catStyle = StyleMapper.getCategoryStyle(catDto.name)
                val catServices = services.filter { it.categoryId == catDto.id }
                    .sortedBy { it.sortOrder }
                    .map { sDto ->
                        val sStyle = StyleMapper.getServiceStyle(sDto.name, catStyle.color)
                        ServiceItem(
                            id = sDto.id, title = sDto.name, description = sDto.shortDescription,
                            icon = if (sDto.icon != null) IconMapper.map(sDto.icon) else sStyle.icon,
                            color = sStyle.color, isActive = sDto.isActive
                        )
                    }

                ServiceCategory(
                    id = catDto.id, title = catDto.name,
                    icon = if (catDto.icon != null) IconMapper.map(catDto.icon) else catStyle.icon,
                    color = catStyle.color, services = catServices
                )
            }
            
            cachedCategories = mapped
            lastCacheTime = System.currentTimeMillis()
            mapped
        } catch (e: Exception) {
            cachedCategories ?: emptyList()
        }
    }

    override suspend fun getServices(categoryId: String): List<ServiceItem> = getCategories().find { it.id == categoryId }?.services ?: emptyList()

    override suspend fun getServiceForm(serviceId: String): String? {
        return try {
            val dto = client.from("service_forms").select { 
                filter { eq("service_id", serviceId); eq("is_active", true) }
                order("version", Order.DESCENDING); limit(1)
            }.decodeSingleOrNull<ServiceFormDto>()
            dto?.schema?.toString()
        } catch (e: Exception) { null }
    }

    override suspend fun createOrder(
        serviceId: String,
        priority: PriorityLevel,
        formData: String?,
        clientRequestId: String
    ): Result<ActiveService> {
        val startTime = System.currentTimeMillis()
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))

            val guestKey = getOrCreateGuestKey()
            val customerPhone = preferenceManager.customerMobile.first() ?: ""
            val customerName = preferenceManager.customerName.first() ?: ""
            val customerEmail = preferenceManager.customerEmail.first() ?: ""
            
            // Prevent creating second order if one is already active
            val existing = getMyActiveOrder(guestKey).getOrNull()
            if (existing != null) {
                Log.i(TAG, "Returning existing active order: ${existing.orderId}")
                return Result.success(existing)
            }

            val priorityValue = when (priority) {
                PriorityLevel.NORMAL -> "normal"
                PriorityLevel.FAST -> "fast"
                PriorityLevel.URGENT -> "urgent"
            }

            val formDataJson = try {
                if (!formData.isNullOrBlank()) Json.parseToJsonElement(formData).jsonObject else buildJsonObject {}
            } catch (e: Exception) { buildJsonObject {} }

            val finalFormData = buildJsonObject {
                formDataJson.forEach { (k, v) -> 
                    if (k != "mobile" && k != "mobile_number" && k != "full_name" && k != "name" && k != "email") {
                        put(k, v)
                    }
                }
                put("guest_key", guestKey)
                put("phone", customerPhone)
                put("full_name", customerName)
                if (customerEmail.isNotBlank()) {
                    put("email", customerEmail)
                }
            }
            
            val response = client.postgrest.rpc("create_order", buildJsonObject {
                put("p_service_id", serviceId)
                put("p_priority", priorityValue)
                put("p_form_data", finalFormData)
                put("p_client_request_id", clientRequestId)
            })

            val orderDto = response.decodeAs<OrderDto>()
            val orderId = orderDto.id ?: ""
            
            // Re-sync using getMyActiveOrder to get the full summary (and admin if assigned)
            val active = getMyActiveOrder(guestKey).getOrNull() ?: ActiveService(
                accessId = orderId, orderId = orderId,
                conversationId = null, serviceId = orderDto.serviceId ?: "",
                serviceName = orderDto.serviceName ?: "خدمت",
                trackingNumber = orderDto.customerOrderNumber, priority = orderDto.priority ?: "normal",
                accessToken = "", status = orderDto.status ?: "pending",
                createdAt = parseCreatedAt(orderDto.createdAt) ?: System.currentTimeMillis(),
                lastValidatedAt = System.currentTimeMillis(),
                profileId = orderDto.profileId,
                customerFullName = orderDto.customerFullName,
                customerEmail = orderDto.customerEmail,
                deleteAfter = parseCreatedAt(orderDto.deleteAfter),
                result = orderDto.result
            )
            
            preferenceManager.saveActiveService(active.serviceId, active.accessToken, active.accessId, active.conversationId)
            preferenceManager.saveFullActiveService(json.encodeToString(active))
            secureManager.saveService(active)
            
            logOp("create_order", clientRequestId, serviceId, System.currentTimeMillis() - startTime, "SUCCESS")
            Result.success(active)
        } catch (e: Exception) {
            Log.e(TAG, "Order creation failed")
            Result.failure(e)
        }
    }

    override suspend fun getMyActiveOrder(guestKey: String): Result<ActiveService?> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            
            val response = client.postgrest.rpc("get_my_active_order_summary", buildJsonObject {
                put("p_customer_key", JsonPrimitive(guestKey))
            })
            
            val summary = try { response.decodeAs<ActiveOrderSummaryDto>() } catch (e: Exception) { null }

            if (summary?.order != null && summary.order.id != null) {
                val orderDto = summary.order
                val adminDto = summary.admin
                val orderId = orderDto.id
                
                val conversationId = orderDto.conversationId ?: run {
                    Log.d(TAG, "OrderRepository: conversation lookup fallback for orderId=$orderId")
                    getConversationForOrder(orderId).getOrNull()
                }

                if (orderDto.conversationId != null) {
                    Log.d(TAG, "OrderRepository: conversation_id received from server")
                }
                Log.d(TAG, "OrderRepository: orderId=$orderId")
                
                val active = ActiveService(
                    accessId = orderId, orderId = orderId,
                    conversationId = conversationId,
                    serviceId = orderDto.serviceId ?: "", serviceName = orderDto.serviceName ?: "خدمت",
                    trackingNumber = orderDto.customerOrderNumber, priority = orderDto.priority ?: "normal",
                    accessToken = "", status = orderDto.status ?: "pending",
                    createdAt = parseCreatedAt(orderDto.createdAt) ?: System.currentTimeMillis(),
                    lastValidatedAt = System.currentTimeMillis(),
                    profileId = orderDto.profileId,
                    adminName = adminDto?.fullName,
                    adminAvatar = adminDto?.avatarUrl,
                    adminRole = adminDto?.roleTitle,
                    customerFullName = orderDto.customerFullName,
                    customerEmail = orderDto.customerEmail,
                    deleteAfter = parseCreatedAt(orderDto.deleteAfter),
                    result = orderDto.result,
                    serviceAmount = orderDto.serviceAmount,
                    speedAmount = orderDto.speedAmount,
                    discountAmount = orderDto.discountAmount,
                    totalAmount = orderDto.totalAmount
                )
                Result.success(active)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active order summary")
            Result.failure(e)
        }
    }

    private fun parseCreatedAt(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            kotlinx.datetime.Instant.parse(dateStr).toEpochMilliseconds()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun restoreActiveServices(): Result<List<ActiveService>> {
        val guestKey = getOrCreateGuestKey()
        return getLatestOrderEvent(guestKey).map { event ->
            if (event?.order != null) {
                // If it's cancelled by customer, don't show it as active/restored
                if (event.order.status == "cancelled") {
                    emptyList<ActiveService>()
                } else {
                    listOf(event.order)
                }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getActiveOrderByPhone(phone: String): Result<ActiveService?> = Result.success(null)

    override suspend fun validateService(accessToken: String): Result<ActiveService> = Result.failure(Exception("DEPRECATED"))

    override suspend fun bindOrderAccess(accessToken: String): Result<BindOrderResult> = Result.failure(Exception("DEPRECATED"))

    override fun getMessages(conversationId: String, profileId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (!ensureAuthSession()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        var currentList = emptyList<ChatMessage>()
        Log.d(TAG, "OrderRepository: subscribing to messages conversation=$conversationId")
        
        val channel = client.realtime.channel("chat_$conversationId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") { 
            table = "messages" 
            filter(FilterOperation("conversation_id", FilterOperator.EQ, conversationId))
        }
        
        val fetchMessages = suspend {
            try {
                val messages = client.from("messages").select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", Order.ASCENDING)
                }.decodeAs<List<MessageDto>>()
                
                val newMessages = messages.map { it.toDomain(profileId) }
                // Use a more thorough equality check or just always update if fetched manually
                val merged = (currentList + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                
                currentList = merged
                trySend(currentList)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching messages")
            }
        }

        val statusJob = launch {
            client.realtime.status.collect { status ->
                if (status == Realtime.Status.CONNECTED) {
                    Log.d(TAG, "Chat Realtime connected/reconnected for $conversationId")
                    fetchMessages()
                }
            }
        }

        val eventJob = launch {
            changeFlow.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert -> {
                            val newMsgDto = json.decodeFromJsonElement<MessageDto>(action.record)
                            val newMsg = newMsgDto.toDomain(profileId)
                            if (!currentList.any { it.id == newMsg.id }) {
                                currentList = (currentList + newMsg).sortedBy { it.timestamp }
                                trySend(currentList)
                            }
                        }
                        is PostgresAction.Update -> {
                            val updatedMsgDto = json.decodeFromJsonElement<MessageDto>(action.record)
                            val updatedMsg = updatedMsgDto.toDomain(profileId)
                            currentList = currentList.map { if (it.id == updatedMsg.id) updatedMsg else it }
                            trySend(currentList)
                        }
                        is PostgresAction.Delete -> {
                            fetchMessages() // Simplest way to handle deletes
                        }
                        else -> {}
                    }
                } catch (e: Exception) { 
                    Log.e(TAG, "Error processing realtime message")
                }
            }
        }
        
        channel.subscribe()
        fetchMessages() // Initial fetch

        awaitClose { 
            statusJob.cancel()
            eventJob.cancel()
            runBlocking {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) { }
            }
        }
    }

    override suspend fun sendMessage(conversationId: String, body: String, senderId: String): Result<ChatMessage> {
        if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
        return try {
            val msgDto = MessageDto(conversationId = conversationId, senderId = senderId, body = body)
            val inserted = client.from("messages").insert(msgDto).decodeSingle<MessageDto>()
            Result.success(inserted.toDomain(senderId))
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun observeOrderDeletions(): Flow<String> = emptyFlow()
    
    override fun observeOrderUpdates(): Flow<ActiveService> = callbackFlow {
        if (!ensureAuthSession()) {
            close()
            return@callbackFlow
        }
        val guestKey = getOrCreateGuestKey()
        
        Log.d(TAG, "OrderRepository: starting observeOrderUpdates for guest=$guestKey")
        val orderChannel = client.realtime.channel("order_tracking_$guestKey")
        val ordersFlow = orderChannel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "orders" }
        
        val resyncAndSend = suspend {
            try {
                getMyActiveOrder(guestKey).onSuccess { active ->
                    if (active != null) {
                        Log.d(TAG, "OrderRepository: emitting updated order ${active.orderId}, status=${active.status}")
                        trySend(active)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in resyncAndSend")
            }
        }

        val statusJob = launch {
            client.realtime.status.collect { status ->
                if (status == Realtime.Status.CONNECTED) {
                    Log.d(TAG, "Order Realtime connected, resyncing...")
                    resyncAndSend()
                }
            }
        }

        val eventJob = launch {
            ordersFlow.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            val record = if (action is PostgresAction.Insert) action.record else (action as PostgresAction.Update).record
                            val orderDto = json.decodeFromJsonElement<OrderDto>(record)
                            val orderGuestKey = orderDto.formData?.get("guest_key")?.jsonPrimitive?.content
                            
                            if (orderGuestKey == guestKey) {
                                Log.d(TAG, "Order Realtime UPDATE detected for guestKey")
                                resyncAndSend()
                            }
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.jsonPrimitive?.content
                            Log.d(TAG, "Order Realtime DELETE detected")
                            // We need to notify that this specific order is gone
                            // For simplicity, we trigger a resync which will return null if no order is active
                            resyncAndSend()
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing order event")
                }
            }
        }

        orderChannel.subscribe()
        resyncAndSend() 

        awaitClose {
            statusJob.cancel()
            eventJob.cancel()
            runBlocking { 
                try {
                    orderChannel.unsubscribe() 
                } catch (e: Exception) {}
            }
        }
    }

    override fun observeConversations(orderId: String): Flow<BindOrderResult> = emptyFlow()

    override suspend fun getConversationForOrder(orderId: String): Result<String?> {
        val retries = listOf(300L, 700L, 1500L)
        var attempt = 0
        
        while (true) {
            try {
                val conv = client.from("conversations").select {
                    filter { eq("order_id", orderId) }
                    limit(1)
                }.decodeSingleOrNull<ConversationDto>()
                
                if (conv != null) {
                    Log.d(TAG, "OrderRepository: conversation resolved")
                    return Result.success(conv.id)
                }
                
                if (attempt >= retries.size) {
                    Log.w(TAG, "OrderRepository: conversation lookup fallback failed after ${retries.size} attempts for orderId=$orderId")
                    return Result.success(null)
                }
                
                Log.d(TAG, "OrderRepository: conversation retry attempt=${attempt + 1}")
                delay(retries[attempt])
                attempt++
            } catch (e: Exception) {
                Log.e(TAG, "OrderRepository: conversation lookup error")
                if (attempt >= retries.size) return Result.failure(e)
                delay(retries[attempt])
                attempt++
            }
        }
    }

    override fun observeServices(): Flow<Unit> = emptyFlow()
    override fun observeRealtimeStatus(): Flow<String> = client.realtime.status.map { it.name }

    override fun observeFormRequests(conversationId: String): Flow<List<FormRequestDto>> = callbackFlow {
        if (!ensureAuthSession()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        var currentForms = emptyList<FormRequestDto>()
        var activeChannel: RealtimeChannel? = null
        
        val job = launch {
            Log.d(TAG, "OrderRepository: observeFormRequests for conversationId=$conversationId")
            
            val channel = client.realtime.channel("forms_$conversationId")
            activeChannel = channel
            
            val formsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "form_requests"
                filter(FilterOperation("conversation_id", FilterOperator.EQ, conversationId))
            }

            val fetchForms = suspend {
                try {
                    val forms = client.from("form_requests").select {
                        filter { eq("conversation_id", conversationId) }
                        order("created_at", Order.ASCENDING)
                    }.decodeAs<List<FormRequestDto>>()
                    
                    currentForms = forms
                    trySend(currentForms)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching forms")
                }
            }

            launch {
                client.realtime.status.collect { status ->
                    if (status == Realtime.Status.CONNECTED) {
                        Log.d(TAG, "Forms Realtime connected for $conversationId. Resyncing...")
                        fetchForms()
                    }
                }
            }

            launch {
                formsFlow.collect { action ->
                    try {
                        when (action) {
                            is PostgresAction.Insert -> {
                                val newForm = json.decodeFromJsonElement<FormRequestDto>(action.record)
                                if (!currentForms.any { it.id == newForm.id }) {
                                    currentForms = (currentForms + newForm).sortedBy { it.createdAt }
                                    trySend(currentForms)
                                }
                            }
                            is PostgresAction.Update -> {
                                val updatedForm = json.decodeFromJsonElement<FormRequestDto>(action.record)
                                currentForms = currentForms.map { if (it.id == updatedForm.id) updatedForm else it }
                                trySend(currentForms)
                            }
                            is PostgresAction.Delete -> {
                                val deletedId = action.oldRecord["id"]?.jsonPrimitive?.content
                                currentForms = currentForms.filter { it.id != deletedId }
                                trySend(currentForms)
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error merging form event")
                        fetchForms() // Fallback to full fetch on error
                    }
                }
            }

            channel.subscribe()
            fetchForms()
        }
        
        awaitClose { 
            job.cancel()
            runBlocking {
                try {
                    activeChannel?.unsubscribe()
                } catch (e: Exception) { }
            }
        }
    }

    override fun observeFormResponses(conversationId: String): Flow<List<FormResponseDto>> = observeFormRequests(conversationId).map { requests ->
        requests.mapNotNull { req ->
            val resp = req.response
            if (resp is JsonObject) {
                FormResponseDto(
                    id = null,
                    requestId = req.id,
                    orderId = "", // Deprecated
                    data = resp,
                    createdAt = req.submittedAt ?: req.createdAt
                )
            } else null
        }
    }

    override suspend fun submitFormResponse(requestId: String, orderId: String, data: String): Result<Unit> {
        return try {
            if (!ensureAuthSession()) {
                return Result.failure(Exception("خطا در احراز هویت. نشست کاربری قبلی بازیابی نشد. لطفاً اپلیکیشن را کاملاً بسته و دوباره باز کنید."))
            }
            
            val session = client.auth.currentSessionOrNull()
            if (session == null) {
                return Result.failure(Exception("خطا: نشست معتبری یافت نشد."))
            }

            val responseJson = Json.parseToJsonElement(data).jsonObject
            
            // Log non-sensitive IDs for tracing
            Log.d(TAG, "submitFormResponse: form_id=$requestId, order_id=$orderId")
            
            client.postgrest.rpc("submit_form_response", buildJsonObject {
                put("p_form_id", requestId)
                put("p_response", responseJson)
            })
            
            Result.success(Unit)
        } catch (e: Exception) {
            val errorStr = e.message ?: ""
            val code = if (e is RestException) e.error else {
                when {
                    errorStr.contains("invalid_vehicle_plate") -> "invalid_vehicle_plate"
                    errorStr.contains("form_expired") -> "form_expired"
                    errorStr.contains("invalid_timed_code") -> "invalid_timed_code"
                    errorStr.contains("file_not_found") -> "file_not_found"
                    errorStr.contains("file_too_large") -> "file_too_large"
                    errorStr.contains("form_not_pending") -> "form_not_pending"
                    else -> ""
                }
            }
            
            val errorMsg = if (code.isNotBlank()) {
                when (code) {
                    "not_allowed" -> "خطای عدم دسترسی (not_allowed). هویت فعلی شما با مالک این فرم مطابقت ندارد."
                    "guest_session_mismatch" -> "خطای عدم تطابق نشست. لطفاً دوباره وارد شوید."
                    "form_not_pending" -> "این فرم در وضعیت معلق نیست یا قبلاً ثبت شده است."
                    "form_expired" -> "مهلت این فرم به پایان رسیده است."
                    "invalid_vehicle_plate" -> "شماره پلاک واردشده معتبر نیست."
                    "invalid_timed_code" -> "رمز واردشده صحیح نیست."
                    "file_not_found" -> "فایل‌های پیوست یافت نشدند."
                    "file_too_large" -> "حجم فایل بیشتر از حد مجاز است."
                    else -> "خطا در ارسال فرم: $code"
                }
            } else if (e is RestException) {
                val c = e.error
                when {
                    c.startsWith("required_field_missing:") -> "تکمیل تمامی فیلدهای الزامی ضروری است."
                    c.startsWith("invalid_numeric_field:") -> "مقدار عددی وارد شده معتبر نیست."
                    else -> "خطا در ارسال فرم"
                }
            } else {
                "خطا در ارسال فرم"
            }
            Log.e(TAG, "RPC submit_form_response failed: $code")
            Result.failure(Exception(errorMsg))
        }
    }

    override suspend fun uploadFile(bucket: String, path: String, data: ByteArray, mimeType: String): Result<String> {
        val pathParts = path.split("/")
        val orderId = pathParts.getOrNull(0) ?: "unknown"
        val formRequestId = pathParts.getOrNull(1) ?: "unknown"
        val fileName = pathParts.getOrNull(pathParts.size - 1) ?: "unknown"
        val fileId = fileName.substringBeforeLast(".", fileName)
        val extension = fileName.substringAfterLast(".", "none")

        try {
            if (!ensureAuthSession()) {
                Log.e(TAG, "STORAGE_UPLOAD_PRECHECK_FAILED: unauthenticated")
                return Result.failure(Exception("unauthenticated"))
            }

            val session = client.auth.currentSessionOrNull()
            if (session == null) {
                Log.e(TAG, "STORAGE_UPLOAD_PRECHECK_FAILED: unauthenticated")
                return Result.failure(Exception("unauthenticated"))
            }

            val isAnonymous = session.user?.email == null && session.user?.phone == null

            Log.d(TAG, "STORAGE_UPLOAD_PRECHECK: authenticated=true, userId=${session.user?.id}, anonymous=$isAnonymous, orderId=$orderId, formRequestId=$formRequestId, fileId=$fileId, bucket=$bucket, extension=$extension, mimeType=$mimeType, file_size=${data.size}")

            if (isAnonymous) {
                return Result.failure(Exception("anonymous_session"))
            }

            if (data.isEmpty()) {
                return Result.failure(Exception("invalid_file_type"))
            }

            val bucketApi = client.storage.from(bucket)
            
            bucketApi.upload(path, data) {
                upsert = false
            }
            
            Log.i(TAG, "STORAGE_UPLOAD_SUCCESS: path=$path")
            return Result.success(path)
        } catch (e: Exception) {
            val isRest = e is RestException
            val statusCode = if (e is RestException) e.statusCode else 0
            val errorBody = if (e is RestException) e.error else ""
            val message = e.message ?: ""
            
            Log.e(TAG, "STORAGE_UPLOAD_RAW_ERROR: isRest=$isRest, statusCode=$statusCode, error=$errorBody, message=$message, orderId=$orderId, formRequestId=$formRequestId, fileId=$fileId, bucket=$bucket, extension=$extension, mimeType=$mimeType, size=${data.size}, authenticated=${client.auth.currentSessionOrNull() != null}, anonymous=${client.auth.currentSessionOrNull()?.user?.email == null}, userId=${client.auth.currentSessionOrNull()?.user?.id}, upsert=false, operation=upload")

            val mapped = when {
                statusCode == 413 || message.contains("413") || message.contains("Payload Too Large") || errorBody.contains("too large") -> "file_too_large"
                statusCode == 404 || errorBody.contains("404") || message.contains("bucket not found") -> "bucket_not_found"
                statusCode == 403 || statusCode == 401 || errorBody.contains("403") || errorBody.contains("401") || errorBody.contains("policy") || message.contains("policy") || message.contains("P0001") || errorBody.contains("P0001") -> {
                    when {
                        message.contains("form_request") || errorBody.contains("form_request") -> "form_not_pending"
                        message.contains("order") || errorBody.contains("order") -> "order_not_owned"
                        else -> "storage_policy_denied"
                    }
                }
                message.contains("path") || message.contains("invalid path") -> "invalid_path"
                message.contains("network") || message.contains("timeout") || message.contains("Connect") -> "network_error"
                else -> "upload_failed"
            }
            return Result.failure(Exception(mapped))
        }
    }

    override suspend fun registerFormFile(
        formRequestId: String,
        originalName: String,
        storagePath: String,
        mimeType: String,
        fileSize: Long
    ): Result<String> {
        return try {
            if (!ensureAuthSession()) {
                return Result.failure(Exception("خطا در احراز هویت. نشست کاربری قبلی بازیابی نشد."))
            }

            Log.d(TAG, "registerFormFile: form_request_id=$formRequestId")
            
            val response = client.postgrest.rpc("register_form_file", buildJsonObject {
                put("p_form_request_id", formRequestId)
                put("p_original_name", originalName)
                put("p_storage_path", storagePath)
                put("p_mime_type", mimeType)
                put("p_file_size", fileSize)
            })
            
            val fileId = response.decodeAs<String>()
            Result.success(fileId)
        } catch (e: Exception) {
            val errorMsg = if (e is RestException) {
                Log.e(TAG, "FILE_REGISTER_FAILED: ${e.error}")
                "خطا در ثبت فایل: ${e.message}"
            } else {
                Log.e(TAG, "FILE_REGISTER_FAILED: unknown")
                "خطای غیرمنتظره در ثبت فایل."
            }
            Result.failure(Exception(errorMsg))
        }
    }

    private fun MessageDto.toDomain(currentUserId: String) = ChatMessage(
        id = id ?: "", requestId = "", 
        senderRole = when {
            messageType == "system" -> "SYSTEM"
            senderId != null && senderId == currentUserId -> "CUSTOMER"
            else -> "ADMIN"
        },
        content = body ?: "",
        timestamp = parseCreatedAt(createdAt) ?: System.currentTimeMillis(),
        type = messageType,
        attachments = emptyList()
    )

    override suspend fun cancelService(orderId: String): Result<Unit> {
        Log.i(TAG, "CancelOrder: calling cancel_order, orderId=$orderId")
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            client.postgrest.rpc("cancel_order", buildJsonObject { put("p_order_id", JsonPrimitive(orderId)) })
            Log.i(TAG, "CancelOrder: success, orderId=$orderId. Clearing local access.")
            clearLocalAccess()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorDetails = if (e is RestException) {
                "status=${e.statusCode}, error=${e.error}"
            } else {
                "type=${e::class.java.simpleName}"
            }
            Log.e(TAG, "CancelOrder: FAILED, $errorDetails")
            // Even if RPC fails, if it's because order is already gone (404), we should clear local
            if (e is RestException && e.statusCode == 404) {
                clearLocalAccess()
                return Result.success(Unit)
            }
            Result.failure(e)
        }
    }

    override suspend fun getLatestOrderEvent(guestKey: String): Result<LatestOrderEvent?> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            val response = client.postgrest.rpc("get_my_latest_order_event", buildJsonObject {
                put("p_customer_key", JsonPrimitive(guestKey))
            })
            
            val dto = response.decodeAs<LatestOrderEventDto>()
            
            val orderDto = dto.order
            val activeOrder = if (orderDto?.id != null) {
                val conversationId = orderDto.conversationId ?: run {
                    Log.d(TAG, "OrderRepository: conversation lookup fallback")
                    getConversationForOrder(orderDto.id).getOrNull()
                }

                ActiveService(
                    accessId = orderDto.id, orderId = orderDto.id,
                    conversationId = conversationId,
                    serviceId = orderDto.serviceId ?: "", serviceName = orderDto.serviceName ?: "خدمت",
                    trackingNumber = orderDto.customerOrderNumber, priority = orderDto.priority ?: "normal",
                    accessToken = "", status = orderDto.status ?: "pending",
                    createdAt = parseCreatedAt(orderDto.createdAt) ?: System.currentTimeMillis(),
                    lastValidatedAt = System.currentTimeMillis(),
                    profileId = orderDto.profileId,
                    customerFullName = orderDto.customerFullName,
                    customerEmail = orderDto.customerEmail,
                    deleteAfter = parseCreatedAt(orderDto.deleteAfter),
                    result = orderDto.result,
                    serviceAmount = orderDto.serviceAmount,
                    speedAmount = orderDto.speedAmount,
                    discountAmount = orderDto.discountAmount,
                    totalAmount = orderDto.totalAmount
                )
            } else null
            
            val lastMessage = dto.lastMessage?.toDomain(orderDto?.profileId ?: "")
            
            Result.success(LatestOrderEvent(
                order = activeOrder,
                lastMessage = lastMessage,
                result = dto.result
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest order event")
            Result.failure(e)
        }
    }

    override suspend fun clearLocalAccess() {
        preferenceManager.clearActiveService()
        secureManager.clearAll()
    }

    override suspend fun getSupportChannels(): Result<List<SupportChannel>> {
        return try {
            if (!ensureAuthSession()) return Result.success(emptyList())
            val dtos = client.postgrest.rpc("get_public_support_channels", buildJsonObject {}).decodeAs<List<SupportChannelDto>>()
            val domain = dtos.map { dto ->
                SupportChannel(
                    id = dto.id,
                    type = dto.channelType,
                    identifier = dto.identifier,
                    isActive = true,
                    displayOrder = dto.displayOrder
                )
            }.sortedBy { it.displayOrder }
            Result.success(domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get support channels")
            Result.success(emptyList())
        }
    }

    override fun observeSupportChannels(): Flow<List<SupportChannel>> = flow {
        ensureAuthSession()
        // Initial fetch
        getSupportChannels().onSuccess { emit(it) }.onFailure { emit(emptyList()) }
        
        // Then observe changes
        val channel = client.realtime.channel("public_support_channels")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "support_channels" }
        
        channel.subscribe()
        
        try {
            changeFlow.collect {
                Log.d(TAG, "Support channels changed, refreshing...")
                getSupportChannels().onSuccess { emit(it) }
            }
        } finally {
            withContext(NonCancellable) {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) {
                    Log.e(TAG, "Unsubscribe error")
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getAppContent(key: String): Result<AppContent> {
        return try {
            if (!ensureAuthSession()) return Result.success(AppContent("", ""))
            val response = client.postgrest.rpc("get_public_app_content", buildJsonObject {
                put("p_key", key)
            })
            
            val jsonResponse = try { 
                response.decodeAs<JsonObject>() 
            } catch (e: Exception) {
                try {
                    response.decodeAs<List<JsonObject>>().firstOrNull() ?: buildJsonObject {}
                } catch (e2: Exception) {
                    buildJsonObject {}
                }
            }

            Result.success(AppContent(
                title = jsonResponse["title"]?.jsonPrimitive?.content ?: "",
                body = jsonResponse["body"]?.jsonPrimitive?.content ?: ""
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app content for $key")
            Result.success(AppContent("", ""))
        }
    }

    override fun observeAppContent(key: String): Flow<AppContent> = flow {
        ensureAuthSession()
        // Initial fetch
        getAppContent(key).onSuccess { emit(it) }
        
        // Then observe changes
        val channel = client.realtime.channel("app_settings_$key")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "app_settings"
        }
        
        channel.subscribe()
        
        try {
            changeFlow.collect { action ->
                val record = when (action) {
                    is PostgresAction.Update -> action.record
                    is PostgresAction.Insert -> action.record
                    else -> null
                }
                val recordKey = record?.get("key")?.jsonPrimitive?.content
                if (recordKey == key) {
                    Log.d(TAG, "App content $key changed, refreshing...")
                    getAppContent(key).onSuccess { emit(it) }
                }
            }
        } finally {
            withContext(NonCancellable) {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) { }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getAnnouncements(): Result<List<AppAnnouncement>> {
        return try {
            if (!ensureAuthSession()) return Result.success(emptyList())
            val dtos = client.postgrest.rpc("get_active_announcements", buildJsonObject {}).decodeAs<List<AnnouncementDto>>()
            val domain = dtos.map { dto ->
                AppAnnouncement(
                    id = dto.id,
                    title = dto.title,
                    body = dto.body,
                    publishedAt = dto.publishedAt,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt
                )
            }
            Result.success(domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get announcements")
            Result.success(emptyList())
        }
    }

    override suspend fun recordLogin(): Result<Unit> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (ensureAuthSession()) {
                    client.postgrest.rpc("record_my_login", buildJsonObject {})
                    Log.d(TAG, "Login recorded successfully (background)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record login (background)")
            }
        }
        return Result.success(Unit)
    }

    override suspend fun touchPresence(clientId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        if (now - lastPresenceTouchTime < 20000) {
            Log.d(TAG, "Heartbeat throttled, skipping.")
            return Result.success(Unit)
        }

        if (!isPresenceJobRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Previous heartbeat is still running, skipping new heartbeat.")
            return Result.success(Unit)
        }

        presenceScope.launch {
            try {
                if (authMutex.isLocked) {
                    Log.d(TAG, "Heartbeat skipped: authMutex is locked.")
                    return@launch
                }

                val currentStatus = client.auth.sessionStatus.value
                if (currentStatus is SessionStatus.Initializing) {
                    Log.d(TAG, "Heartbeat skipped: auth session is initializing.")
                    return@launch
                }

                val session = client.auth.currentSessionOrNull()
                val isExpired = session?.expiresAt?.let { it < kotlinx.datetime.Clock.System.now() } ?: true
                if (session == null || isExpired) {
                    Log.d(TAG, "Heartbeat skipped: session is invalid or expired.")
                    return@launch
                }

                withTimeout(8000) {
                    client.postgrest.rpc("touch_my_presence", buildJsonObject {
                        put("p_client_id", clientId)
                    })
                }
                lastPresenceTouchTime = System.currentTimeMillis()
                Log.d(TAG, "Heartbeat presence touched successfully.")
            } catch (e: Exception) {
                Log.w(TAG, "Heartbeat RPC failed or timed out: ${e.message}")
            } finally {
                isPresenceJobRunning.set(false)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun clearPresence(clientId: String): Result<Unit> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            client.postgrest.rpc("clear_my_presence", buildJsonObject {
                put("p_client_id", clientId)
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAdminUserPresence(): Result<List<AdminUserPresence>> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            val dtos = client.postgrest.rpc("get_admin_user_presence", buildJsonObject {}).decodeAs<List<AdminUserPresenceDto>>()
            val domain = dtos.map { dto ->
                AdminUserPresence(
                    id = dto.id,
                    fullName = dto.fullName,
                    roleTitle = dto.roleTitle,
                    isActive = dto.isActive,
                    lastLoginAt = dto.lastLoginAt,
                    isOnline = dto.isOnline
                )
            }
            Result.success(domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get admin user presence")
            Result.failure(e)
        }
    }

    override fun observeAnnouncements(): Flow<List<AppAnnouncement>> = flow {
        ensureAuthSession()
        // Initial fetch
        getAnnouncements().onSuccess { emit(it) }.onFailure { emit(emptyList()) }
        
        val channel = client.realtime.channel("public_announcements")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "app_announcements" }
        
        channel.subscribe()
        
        try {
            changeFlow.collect {
                Log.d(TAG, "Announcements changed, refreshing...")
                getAnnouncements().onSuccess { emit(it) }
            }
        } finally {
            withContext(NonCancellable) {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) { }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getNotifications(): Result<List<HamrahyarNotification>> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            val dtos = client.from("notifications").select {
                order("created_at", Order.DESCENDING)
            }.decodeAs<List<NotificationDto>>()
            
            val domain = dtos.map { dto ->
                HamrahyarNotification(
                    id = dto.id ?: "",
                    title = dto.title ?: "بدون عنوان",
                    message = dto.message ?: "",
                    isRead = dto.isRead,
                    createdAt = parseCreatedAt(dto.createdAt) ?: System.currentTimeMillis()
                )
            }
            Result.success(domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get notifications")
            Result.failure(e)
        }
    }

    override suspend fun markNotificationAsRead(id: String): Result<Unit> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            client.from("notifications").update(buildJsonObject { 
                put("is_read", true) 
            }) {
                filter { eq("id", id) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvoiceForOrder(orderId: String): Result<InvoiceDto?> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            val invoice = client.from("invoices").select {
                filter { eq("order_id", orderId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<InvoiceDto>()
            Result.success(invoice)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get invoice")
            Result.failure(e)
        }
    }

    override fun observeInvoiceForOrder(orderId: String): Flow<InvoiceDto?> = callbackFlow {
        if (orderId.isBlank() || !ensureAuthSession()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        var activeChannel: RealtimeChannel? = null

        val fetchInvoice = suspend {
            try {
                getInvoiceForOrder(orderId).onSuccess { invoice ->
                    trySend(invoice)
                }.onFailure {
                    Log.e(TAG, "Failed to fetch invoice for order $orderId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching invoice for order $orderId: ${e.message}")
            }
        }

        val job = launch {
            Log.d(TAG, "OrderRepository: observeInvoiceForOrder for orderId=$orderId")

            val channel = client.realtime.channel("invoice_$orderId")
            activeChannel = channel

            val invoiceFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "invoices"
                filter(FilterOperation("order_id", FilterOperator.EQ, orderId))
            }

            val paymentFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "payments"
                filter(FilterOperation("order_id", FilterOperator.EQ, orderId))
            }

            launch {
                client.realtime.status.collect { status ->
                    if (status == Realtime.Status.CONNECTED) {
                        Log.d(TAG, "Invoice Realtime connected for $orderId. Resyncing...")
                        fetchInvoice()
                    }
                }
            }

            launch {
                invoiceFlow.collect { action ->
                    try {
                        when (action) {
                            is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                                Log.d(TAG, "Invoice Realtime change detected for order $orderId")
                                fetchInvoice()
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing invoice realtime event: ${e.message}")
                        fetchInvoice()
                    }
                }
            }

            launch {
                paymentFlow.collect { action ->
                    try {
                        when (action) {
                            is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                                Log.d(TAG, "Payment Realtime change detected for order $orderId")
                                fetchInvoice()
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing payment realtime event: ${e.message}")
                        fetchInvoice()
                    }
                }
            }

            channel.subscribe()
            fetchInvoice()
        }

        awaitClose {
            job.cancel()
            runBlocking {
                try {
                    activeChannel?.unsubscribe()
                } catch (e: Exception) { }
            }
        }
    }

    override suspend fun getPaymentForInvoice(invoiceId: String): Result<PaymentDto?> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            val payment = client.from("payments").select {
                filter { eq("invoice_id", invoiceId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<PaymentDto>()
            Result.success(payment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get payment")
            Result.failure(e)
        }
    }

    override suspend fun submitCardToCardPayment(
        invoiceId: String,
        amount: Double,
        payerFullName: String,
        payerBank: String,
        payerCardLast4: String,
        paymentTrackingCode: String,
        receiptPath: String?
    ): Result<Unit> {
        return try {
            if (!ensureAuthSession()) return Result.failure(Exception("AUTH_FAILED"))
            client.postgrest.rpc("submit_card_to_card_payment", buildJsonObject {
                put("p_invoice_id", invoiceId)
                put("p_amount", amount)
                put("p_payer_full_name", payerFullName)
                put("p_payer_bank", payerBank)
                put("p_payer_card_last4", payerCardLast4)
                put("p_tracking_code", paymentTrackingCode)
                put("p_receipt_path", receiptPath ?: "")
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit payment")
            Result.failure(e)
        }
    }

    override suspend fun getPriorityConfiguration(): List<PriorityConfig> {
        return try {
            if (!ensureAuthSession()) return emptyList()
            val response = client.postgrest.rpc("get_customer_priority_options", buildJsonObject {})
            val options = response.decodeAs<List<PriorityOptionDto>>()
            options.map { dto ->
                PriorityConfig(
                    level = PriorityLevel.fromValue(dto.priority),
                    additionalCost = dto.amount,
                    description = "زمان تحویل تخمینی: ${dto.minHours} تا ${dto.maxHours} ساعت",
                    estimatedTime = "${dto.minHours} تا ${dto.maxHours} ساعت"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get priority configuration")
            throw e
        }
    }

    private fun logOp(op: String, clientReqId: String = "", serviceId: String = "", duration: Long = 0, result: String = "") {
        Log.i(TAG, "Op: $op, reqId: $clientReqId, serviceId: $serviceId, duration: ${duration}ms, result: $result")
    }
}
