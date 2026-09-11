package com.lucifer.hamrahyar.ui.theme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActiveService(
    val accessId: String,
    val orderId: String,
    val conversationId: String? = null,
    val serviceId: String,
    val serviceName: String,
    val trackingNumber: String? = null,
    val priority: String,
    val accessToken: String,
    val status: String,
    val createdAt: Long,
    val lastValidatedAt: Long,
    val profileId: String? = null,
    val adminName: String? = null,
    val adminAvatar: String? = null,
    val adminRole: String? = null,
    val customerFullName: String? = null,
    val customerEmail: String? = null,
    val deleteAfter: Long? = null,
    val result: String? = null,
    val serviceAmount: Double = 0.0,
    val speedAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)
