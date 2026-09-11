package com.lucifer.hamrahyar.ui.theme.domain.model

import android.util.Log
import androidx.compose.ui.graphics.Color

import kotlinx.serialization.Serializable

enum class ServiceStatus(val value: String, val label: String) {
    SUBMITTED("submitted", "در صف پذیرش"),
    UNDER_REVIEW("under_review", "پذیرش شد"),
    WAITING_DOCUMENTS("waiting_documents", "در انتظار بارگذاری مدارک"),
    INVOICE_ISSUED("invoice_issued", "صورتحساب صادر شد"),
    WAITING_PAYMENT("waiting_payment", "در انتظار تسویه"),
    PAID("paid", "پرداخت شد"),
    PROCESSING("processing", "در حال انجام"),
    READY_DELIVERY("ready_delivery", "در حال ارسال نتیجه"),
    WAITING_CONFIRMATION("waiting_confirmation", "در انتظار تأیید"),
    CLOSED("closed", "تکمیل شد"),
    ARCHIVED("archived", "بایگانی شد"),
    REJECTED("rejected", "رد شده"),
    CANCELLED("cancelled", "لغو شده"),
    UNKNOWN("unknown", "وضعیت سفارش");

    companion object {
        fun fromValue(value: String): ServiceStatus {
            val normalized = value.lowercase().trim()
            val status = values().find { it.value == normalized || (it.value == "submitted" && normalized == "pending") }
            if (status == null) {
                Log.w("ServiceStatus", "Unknown status received from backend: $value")
                return UNKNOWN
            }
            return status
        }
    }
}

enum class PriorityLevel(val value: String, val label: String, val color: Color) {
    NORMAL("normal", "عادی", Color(0xFF2196F3)),
    FAST("fast", "سریع", Color(0xFFFF9800)),
    URGENT("urgent", "خیلی فوری", Color(0xFFF44336));

    companion object {
        fun fromValue(value: String): PriorityLevel {
            val normalized = value.lowercase().trim()
            return values().find { it.value == normalized } ?: NORMAL
        }
    }
}

data class PriorityConfig(
    val level: PriorityLevel,
    val additionalCost: Long,
    val description: String,
    val estimatedTime: String
)

data class ServiceRequest(
    val id: String,
    val trackingNumber: String,
    val categoryId: String,
    val subServiceId: String,
    val customerMobile: String,
    val status: ServiceStatus,
    val priority: PriorityLevel,
    val createdAt: Long,
    val lastUpdate: Long,
    val conversationId: String? = null,
    val serviceName: String? = null,
    val accessToken: String? = null,
    val profileId: String? = null,
    val adminName: String? = null,
    val adminAvatar: String? = null,
    val adminRole: String? = null,
    val customerFullName: String? = null,
    val customerEmail: String? = null,
    val result: String? = null,
    val deleteAfter: Long? = null,
    val serviceAmount: Double = 0.0,
    val speedAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

data class ChatMessage(
    val id: String,
    val requestId: String,
    val senderRole: String, // "ADMIN" or "CUSTOMER" or "SYSTEM"
    val content: String,
    val timestamp: Long,
    val type: String = "text", // "text" or "form"
    val attachments: List<String> = emptyList(),
    val isRead: Boolean = false
)

data class LatestOrderEvent(
    val order: ActiveService? = null,
    val lastMessage: ChatMessage? = null,
    val result: String? = null
)

data class UserProfile(
    val id: String,
    val mobile: String,
    val fullName: String? = null
)

@Serializable
data class HamrahyarNotification(
    val id: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)

@Serializable
data class SupportChannel(
    val id: String,
    val type: String, // telegram, eitaa, rubika, email
    val identifier: String,
    val isActive: Boolean,
    val displayOrder: Int
)

@Serializable
data class AppContent(
    val title: String,
    val body: String
)

@Serializable
data class AppAnnouncement(
    val id: String,
    val title: String,
    val body: String,
    val publishedAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class AdminUserPresence(
    val id: String,
    val fullName: String?,
    val roleTitle: String?,
    val isActive: Boolean,
    val lastLoginAt: String?,
    val isOnline: Boolean
)

data class BindOrderResult(
    val profileId: String,
    val orderId: String,
    val conversationId: String? = null
)
