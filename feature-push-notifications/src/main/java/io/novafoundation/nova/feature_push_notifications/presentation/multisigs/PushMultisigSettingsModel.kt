package io.novafoundation.nova.feature_push_notifications.presentation.multisigs

import android.os.Parcelable
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import kotlinx.parcelize.Parcelize

@Parcelize
data class PushMultisigSettingsModel(
    val isEnabled: Boolean,
    val isInitiatingEnabled: Boolean,
    val isApprovingEnabled: Boolean,
    val isExecutionEnabled: Boolean,
    val isRejectionEnabled: Boolean
) : Parcelable

fun PushMultisigSettingsModel.toDomain() = PushSettings.MultisigsState(
    isEnabled = isEnabled,
    isInitiatingEnabled = isInitiatingEnabled,
    isApprovingEnabled = isApprovingEnabled,
    isExecutionEnabled = isExecutionEnabled,
    isRejectionEnabled = isRejectionEnabled
)

fun PushSettings.MultisigsState.toModel() = PushMultisigSettingsModel(
    isEnabled = isEnabled,
    isInitiatingEnabled = isInitiatingEnabled,
    isApprovingEnabled = isApprovingEnabled,
    isExecutionEnabled = isExecutionEnabled,
    isRejectionEnabled = isRejectionEnabled
)
