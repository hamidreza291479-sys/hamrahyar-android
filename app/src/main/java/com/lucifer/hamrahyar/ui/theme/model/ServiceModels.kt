package com.lucifer.hamrahyar.ui.home.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ServiceCategory(
    val id: String,
    val title: String,
    val icon: Any, // Can be ImageVector or String (Emoji)
    val color: Color,
    val services: List<ServiceItem>
)

data class ServiceItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: Any, // Can be ImageVector or String (Emoji)
    val color: Color,
    val isActive: Boolean
)