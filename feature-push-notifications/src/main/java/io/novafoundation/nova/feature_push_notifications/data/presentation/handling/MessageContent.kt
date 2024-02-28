package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

class MessageContent(
    val token: String?,
    val topic: String?,
    val notification: Notification?,
    val data: DataContent?
) {

    class Notification(
        val title: String,
        val image: String
    )

    class DataContent(
        val type: String,
        val chainId: String?,
        val payload: Map<String, Any>
    )
}
