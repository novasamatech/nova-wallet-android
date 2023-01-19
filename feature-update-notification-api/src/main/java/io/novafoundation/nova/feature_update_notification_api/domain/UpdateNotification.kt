package io.novafoundation.nova.feature_update_notification_impl.domain

import java.util.Date

class UpdateNotification(
    val version: String,
    val description: String,
    val severity: Severity,
    val time: Date
)

enum class Severity {
    NORMAL, CRITICAL
}
