package io.novafoundation.nova.feature_push_notifications.data.data.subscription

import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings

interface PushSubscriptionService {

    suspend fun handleSubscription(token: String, pushSettings: PushSettings)
}
