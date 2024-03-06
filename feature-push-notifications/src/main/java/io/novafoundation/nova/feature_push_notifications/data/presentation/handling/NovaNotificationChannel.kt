package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import androidx.annotation.StringRes
import io.novafoundation.nova.feature_push_notifications.R

enum class NovaNotificationChannel(
    @StringRes val idRes: Int,
    @StringRes val nameRes: Int
) {

    DEFAULT(R.string.default_notification_channel_id, R.string.default_notification_channel_name),

    GOVERNANCE(R.string.governance_notification_channel_id, R.string.governance_notification_channel_name),

    TRANSACTIONS(R.string.transactions_notification_channel_id, R.string.transactions_notification_channel_name),

    STAKING(R.string.staking_notification_channel_id, R.string.staking_notification_channel_name)
}
