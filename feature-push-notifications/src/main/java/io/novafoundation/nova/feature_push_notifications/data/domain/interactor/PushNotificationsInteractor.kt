package io.novafoundation.nova.feature_push_notifications.data.domain.interactor

import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService

interface PushNotificationsInteractor {

    suspend fun syncSettingsIfNeeded()
}

class RealPushNotificationsInteractor(
    private val pushNotificationsService: PushNotificationsService
) : PushNotificationsInteractor {

    override suspend fun syncSettingsIfNeeded() {
        if (pushNotificationsService.isNeedToSyncSettings()) {
            pushNotificationsService.syncSettings()
        }
    }
}
