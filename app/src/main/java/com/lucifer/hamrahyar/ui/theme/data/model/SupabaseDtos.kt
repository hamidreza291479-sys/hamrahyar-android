package com.lucifer.hamrahyar.ui.theme.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val slug: String? = null,
    val icon: String? = null,
    val description: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ServiceDto(
    val id: String,
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val slug: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    val description: String? = null,
    @SerialName("estimated_days") val estimatedDays: Int? = null,
    val icon: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ServiceFormDto(
    val id: String,
    @SerialName("service_id") val serviceId: String,
    val version: Int = 1,
    @SerialName("is_active") val isActive: Boolean = true,
    val schema: JsonObject,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class OrderDto(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("service_id") val serviceId: String? = null,
    @SerialName("customer_full_name") val customerFullName: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("service_name") val serviceName: String? = null,
    val priority: String? = null,
    val status: String? = null,
    @SerialName("form_data") val formData: JsonObject? = null,
    @SerialName("customer_order_number") val customerOrderNumber: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("assigned_operator_id") val assignedOperatorId: String? = null,
    @SerialName("fixed_amount") val fixedAmount: Double = 0.0,
    @SerialName("external_amount") val externalAmount: Double = 0.0,
    @SerialName("service_amount") val serviceAmount: Double = 0.0,
    @SerialName("speed_amount") val speedAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("retention_days") val retentionDays: Int = 30,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("security_verified_at") val securityVerifiedAt: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("delete_after") val deleteAfter: String? = null,
    val result: String? = null
)

@Serializable
data class NotificationDto(
    val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    val title: String? = null,
    val message: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RegisterServiceRequest(
    @SerialName("p_service_id") val serviceId: String,
    @SerialName("p_priority") val priority: String,
    @SerialName("p_form_data") val formData: JsonObject,
    @SerialName("p_client_request_id") val clientRequestId: String,
    @SerialName("p_customer_key") val customerKey: String? = null
)

@Serializable
data class SubmitServiceRequestDto(
    @SerialName("p_category_id") val categoryId: String,
    @SerialName("p_name") val name: String,
    @SerialName("p_slug") val slug: String? = null,
    @SerialName("p_short_description") val shortDescription: String? = null,
    @SerialName("p_description") val description: String? = null,
    @SerialName("p_estimated_days") val estimatedDays: Int = 1
)

@Serializable
data class ServiceRequestResponseDto(
    val id: String,
    @SerialName("requested_by") val requestedBy: String,
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ActiveServiceDto(
    val id: String,
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val slug: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    @SerialName("estimated_days") val estimatedDays: Int? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val icon: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ConversationDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("operator_id") val operatorId: String? = null,
    val status: String
)

@Serializable
data class MessageDto(
    val id: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("message_type") val messageType: String = "text",
    val body: String? = null,
    val payload: JsonObject = JsonObject(emptyMap()),
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RegisterServiceResponse(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null,
    val errors: List<FormFieldError>? = null,
    val data: RegisterServiceResponseData? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("access_id") val accessId: String? = null
)

@Serializable
data class FormFieldError(
    val key: String,
    val label: String? = null,
    val message: String? = null
)

class InitialFormIncompleteException(val errors: List<FormFieldError>) : Exception("INITIAL_FORM_INCOMPLETE")

@Serializable
data class RegisterServiceResponseData(
    val order: OrderDto? = null,
    val conversation: ConversationResponseInfo? = null,
    val access: AccessTokenInfo? = null,
    val mobile: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("service_name") val serviceName: String? = null,
    val priority: String? = null
)

@Serializable
data class AccessTokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("access_id") val accessId: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null
)

@Serializable
data class ConversationResponseInfo(
    val id: String
)

@Serializable
data class RestoreAccessRequest(
    val action: String = "restore",
    val accessTokens: List<String>
)

@Serializable
data class RestoreAccessResponse(
    val success: Boolean,
    val code: String? = null,
    val data: RestoreAccessData? = null
)

@Serializable
data class RestoreAccessData(
    val services: List<RegisterServiceResponseData> = emptyList(),
    val invalidAccessTokens: List<String> = emptyList()
)

@Serializable
data class ServiceAccessActionRequest(
    val action: String,
    val body: String? = null,
    @SerialName("message_type") val messageType: String? = null
)

@Serializable
data class CloseServiceRequest(
    val action: String,
    val accessToken: String
)

@Serializable
data class CloseServiceResponse(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class SupportChannelDto(
    val id: String,
    @SerialName("channel_type") val channelType: String,
    val identifier: String,
    @SerialName("display_order") val displayOrder: Int
)

@Serializable
data class ResolvedServiceAccessDto(
    @SerialName("access_id") val accessId: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val status: String? = null,
    @SerialName("service_id") val serviceId: String? = null,
    @SerialName("service_name") val serviceName: String? = null,
    @SerialName("customer_order_number") val customerOrderNumber: String? = null,
    @SerialName("customer_full_name") val customerFullName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    val priority: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val success: Boolean = true,
    val code: String? = null,
    val admin: AdminDto? = null
)

@Serializable
data class AdminDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("role_title") val roleTitle: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class PriorityOptionDto(
    val priority: String,
    val title: String,
    @SerialName("min_hours") val minHours: Int,
    @SerialName("max_hours") val maxHours: Int,
    val amount: Long
)

@Serializable
data class LatestOrderEventDto(
    val order: OrderDto? = null,
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    val result: String? = null
)

@Serializable
data class ActiveOrderSummaryDto(
    val order: OrderDto? = null,
    val admin: AdminDto? = null
)

@Serializable
data class BindOrderResponse(
    val success: Boolean,
    val code: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null
)

@Serializable
data class AnnouncementDto(
    val id: String,
    val title: String,
    val body: String,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class AdminUserPresenceDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("role_title") val roleTitle: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false
)

@Serializable
data class FormRequestDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("component_template_id") val componentTemplateId: String? = null,
    val title: String? = null,
    val schema: JsonObject? = null,
    val response: JsonObject? = null,
    val status: String, // pending, submitted, expired
    @SerialName("requested_by") val requestedBy: String? = null,
    @SerialName("responded_by") val respondedBy: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null
) {
    val prompt: String get() = title ?: ""
    val type: String get() = schema?.get("type")?.jsonPrimitive?.content ?: componentTemplateId ?: "text"
    val orderId: String get() = "" // Deprecated, but kept for compatibility
}

@Serializable
data class FormResponseDto(
    val id: String? = null,
    @SerialName("request_id") val requestId: String,
    @SerialName("order_id") val orderId: String,
    val data: JsonObject,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class InvoiceDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("base_amount") val baseAmount: Double,
    @SerialName("total_amount") val totalAmount: Double,
    val status: String, // unpaid, paid, cancelled
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class PaymentDto(
    val id: String? = null,
    @SerialName("invoice_id") val invoiceId: String,
    @SerialName("order_id") val orderId: String,
    val amount: Double,
    val status: String, // pending_review, approved, rejected
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("tracking_code") val trackingCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
