package com.lucifer.hamrahyar.ui.theme.domain.repository

import com.lucifer.hamrahyar.ui.home.model.ServiceCategory
import com.lucifer.hamrahyar.ui.home.model.ServiceItem
import com.lucifer.hamrahyar.ui.theme.data.model.FormRequestDto
import com.lucifer.hamrahyar.ui.theme.data.model.FormResponseDto
import com.lucifer.hamrahyar.ui.theme.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OnlineServiceRepository {

    suspend fun getCategories(): List<ServiceCategory>

    suspend fun getServices(categoryId: String): List<ServiceItem>

    suspend fun getServiceForm(serviceId: String): String?

    // New Server-Driven Access Methods
    suspend fun createOrder(
        serviceId: String,
        priority: PriorityLevel,
        formData: String? = null,
        clientRequestId: String
    ): Result<ActiveService>

    suspend fun getMyActiveOrder(guestKey: String): Result<ActiveService?>

    suspend fun restoreActiveServices(): Result<List<ActiveService>>

    suspend fun getActiveOrderByPhone(phone: String): Result<ActiveService?>

    suspend fun validateService(accessToken: String): Result<ActiveService>

    suspend fun bindOrderAccess(accessToken: String): Result<BindOrderResult>

    fun getMessages(conversationId: String, profileId: String): Flow<List<ChatMessage>>
    
    suspend fun sendMessage(conversationId: String, body: String, senderId: String): Result<ChatMessage>

    fun observeOrderDeletions(): Flow<String>

    fun observeOrderUpdates(): Flow<ActiveService>

    fun observeConversations(orderId: String): Flow<BindOrderResult>

    suspend fun getConversationForOrder(orderId: String): Result<String?>

    fun observeServices(): Flow<Unit> // Just a trigger to refresh categories

    fun observeRealtimeStatus(): Flow<String>

    fun observeFormRequests(conversationId: String): Flow<List<FormRequestDto>>

    fun observeFormResponses(conversationId: String): Flow<List<FormResponseDto>>
    
    suspend fun submitFormResponse(requestId: String, orderId: String, data: String): Result<Unit>

    suspend fun cancelService(orderId: String): Result<Unit>
    
    suspend fun getLatestOrderEvent(guestKey: String): Result<LatestOrderEvent?>
    
    suspend fun clearLocalAccess()
    
    suspend fun getSupportChannels(): Result<List<SupportChannel>>
    
    suspend fun getAppContent(key: String): Result<AppContent>
    
    suspend fun getAnnouncements(): Result<List<AppAnnouncement>>
    
    fun observeSupportChannels(): Flow<List<SupportChannel>>
    
    fun observeAppContent(key: String): Flow<AppContent>
    
    fun observeAnnouncements(): Flow<List<AppAnnouncement>>
    
    suspend fun getNotifications(): Result<List<HamrahyarNotification>>
    
    suspend fun markNotificationAsRead(id: String): Result<Unit>

    suspend fun recordLogin(): Result<Unit>

    suspend fun touchPresence(clientId: String): Result<Unit>

    suspend fun clearPresence(clientId: String): Result<Unit>

    suspend fun getAdminUserPresence(): Result<List<AdminUserPresence>>
    
    // Legacy support (optional, can be phased out)
    suspend fun getPriorityConfiguration(): List<PriorityConfig>
}
