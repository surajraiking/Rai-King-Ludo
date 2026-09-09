package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val senderColor: LudoColor,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FloatingEmoji(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emoji: String,
    val senderName: String,
    val senderColor: LudoColor,
    val startXFraction: Float,
    val startYFraction: Float,
    val timestamp: Long = System.currentTimeMillis()
)
