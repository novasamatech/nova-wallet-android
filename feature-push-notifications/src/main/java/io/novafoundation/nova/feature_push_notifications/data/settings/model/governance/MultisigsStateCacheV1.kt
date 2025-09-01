package io.novafoundation.nova.feature_push_notifications.data.settings.model.governance

import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings

class MultisigsStateCacheV1(
    val isEnabled: Boolean, // General notifications state. Other states may be enabled to save state when general one is disabled
    val isInitialNotificationsEnabled: Boolean,
    val isApprovalNotificationsEnabled: Boolean,
    val isExecutionNotificationsEnabled: Boolean,
    val isRejectionNotificationsEnabled: Boolean
)

fun MultisigsStateCacheV1.toDomain(): PushSettings.MultisigsState {
    return PushSettings.MultisigsState(
        isEnabled = isEnabled,
        isInitiatingEnabled = isInitialNotificationsEnabled,
        isApprovingEnabled = isApprovalNotificationsEnabled,
        isExecutionEnabled = isExecutionNotificationsEnabled,
        isRejectionEnabled = isRejectionNotificationsEnabled
    )
}
