package io.novafoundation.nova.feature_versions_api.domain

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
