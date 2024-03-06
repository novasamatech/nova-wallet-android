package io.novafoundation.nova.feature_push_notifications.data.domain.interactor

import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import kotlinx.coroutines.flow.Flow

interface PushNotificationsInteractor {

    suspend fun syncSettings()

    fun pushNotificationsEnabledFlow(): Flow<Boolean>

    suspend fun setPushNotificationsEnabled(enable: Boolean): Result<Unit>

    suspend fun updatePushSettings(enable: Boolean, pushSettings: PushSettings): Result<Unit>

    suspend fun getPushSettings(): PushSettings

    fun isPushNotificationsEnabled(): Boolean

    fun isPushNotificationsAvailable(): Boolean
}

class RealPushNotificationsInteractor(
    private val pushNotificationsService: PushNotificationsService,
    private val pushSettingsProvider: PushSettingsProvider
) : PushNotificationsInteractor {

    override suspend fun syncSettings() {
        pushNotificationsService.syncSettings()
    }

    override fun pushNotificationsEnabledFlow(): Flow<Boolean> {
        return pushSettingsProvider.pushEnabledFlow()
    }

    override suspend fun setPushNotificationsEnabled(enable: Boolean): Result<Unit> {
        return pushNotificationsService.initPushNotifications()
    }

    override suspend fun updatePushSettings(enable: Boolean, pushSettings: PushSettings): Result<Unit> {
        return pushNotificationsService.updatePushSettings(enable, pushSettings)
    }

    override suspend fun getPushSettings(): PushSettings {
        return pushSettingsProvider.getPushSettings()
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return pushSettingsProvider.isPushNotificationsEnabled()
    }

    override fun isPushNotificationsAvailable(): Boolean {
        return pushNotificationsService.isPushNotificationsAvailable()
    }
}
