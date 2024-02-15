package io.novafoundation.nova.feature_push_notifications.data.domain.interactor

import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import kotlinx.coroutines.flow.Flow

interface PushNotificationsInteractor {

    suspend fun syncSettingsIfNeeded()

    fun needToShowWelcomeScreen(): Boolean

    fun pushNotificationsEnabledFlow(): Flow<Boolean>

    suspend fun setPushNotificationsEnabled(enable: Boolean): Result<Unit>
}

class RealPushNotificationsInteractor(
    private val pushNotificationsService: PushNotificationsService,
    private val pushSettingsProvider: PushSettingsProvider
) : PushNotificationsInteractor {

    override suspend fun syncSettingsIfNeeded() {
        if (pushNotificationsService.isNeedToSyncSettings()) {
            pushNotificationsService.syncSettings()
        }
    }

    override fun needToShowWelcomeScreen(): Boolean {
        return true // TODO: implement
    }

    override fun pushNotificationsEnabledFlow(): Flow<Boolean> {
        return pushSettingsProvider.pushEnabledFlow()
    }

    override suspend fun setPushNotificationsEnabled(enable: Boolean): Result<Unit> {
        return pushNotificationsService.setPushNotificationsEnabled(enable)
    }
}
