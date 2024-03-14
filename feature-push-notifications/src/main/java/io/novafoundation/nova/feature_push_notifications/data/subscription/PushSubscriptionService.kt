package io.novafoundation.nova.feature_push_notifications.data.subscription

import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings

interface PushSubscriptionService {

    suspend fun handleSubscription(pushEnabled: Boolean, token: String?, oldSettings: PushSettings, newSettings: PushSettings?)
}
