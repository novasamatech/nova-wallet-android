package io.novafoundation.nova.feature_push_notifications.presentation.multisigs

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PushMultisigSettingsModel(
    val isEnabled: Boolean,
    val isInitiatingEnabled: Boolean,
    val isApprovingEnabled: Boolean,
    val isExecutionEnabled: Boolean,
    val isRejectionEnabled: Boolean
) : Parcelable
