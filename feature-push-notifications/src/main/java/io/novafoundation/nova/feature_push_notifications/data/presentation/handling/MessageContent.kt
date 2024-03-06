package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

class NotificationData(
    val type: String,
    val chainId: String?,
    val payload: Map<String, Any>
)
