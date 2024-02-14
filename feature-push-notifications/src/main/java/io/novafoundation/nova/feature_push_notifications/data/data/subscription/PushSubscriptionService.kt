package io.novafoundation.nova.feature_push_notifications.data.data.subscription

import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings

interface PushSubscriptionService {

    suspend fun handleSubscription(pushEnabled: Boolean, token: String?, pushSettings: PushSettings)
}
