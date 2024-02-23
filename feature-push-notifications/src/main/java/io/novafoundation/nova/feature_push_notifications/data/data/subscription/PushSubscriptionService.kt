package io.novafoundation.nova.feature_push_notifications.data.data.subscription

import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings

interface PushSubscriptionService {

    suspend fun handleSubscription(pushEnabled: Boolean, token: String?, oldSettings: PushSettings, newSettings: PushSettings)
}
