package io.novafoundation.nova.feature_push_notifications.domain.interactor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsProvider
import kotlinx.coroutines.flow.Flow

interface PushNotificationsInteractor {

    suspend fun initialSyncSettings()

    fun pushNotificationsEnabledFlow(): Flow<Boolean>

    suspend fun initPushSettings(): Result<Unit>

    suspend fun updatePushSettings(enable: Boolean, pushSettings: PushSettings): Result<Unit>

    suspend fun getPushSettings(): PushSettings

    suspend fun getDefaultSettings(): PushSettings

    fun isPushNotificationsEnabled(): Boolean

    fun isPushNotificationsAvailable(): Boolean

    suspend fun onMetaAccountChanged(metaIds: List<Long>)

    suspend fun onMetaAccountRemoved(metaId: Long)
}

class RealPushNotificationsInteractor(
    private val pushNotificationsService: PushNotificationsService,
    private val pushSettingsProvider: PushSettingsProvider,
    private val accountRepository: AccountRepository
) : PushNotificationsInteractor {

    override suspend fun initialSyncSettings() {
        pushNotificationsService.syncSettingsIfNeeded()
    }

    override fun pushNotificationsEnabledFlow(): Flow<Boolean> {
        return pushSettingsProvider.pushEnabledFlow()
    }

    override suspend fun initPushSettings(): Result<Unit> {
        return pushNotificationsService.initPushNotifications()
    }

    override suspend fun updatePushSettings(enable: Boolean, pushSettings: PushSettings): Result<Unit> {
        return pushNotificationsService.updatePushSettings(enable, pushSettings)
    }

    override suspend fun getPushSettings(): PushSettings {
        return pushSettingsProvider.getPushSettings()
    }

    override suspend fun getDefaultSettings(): PushSettings {
        return pushSettingsProvider.getDefaultPushSettings()
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return pushSettingsProvider.isPushNotificationsEnabled()
    }

    override fun isPushNotificationsAvailable(): Boolean {
        return pushNotificationsService.isPushNotificationsAvailable()
    }

    override suspend fun onMetaAccountChanged(metaIds: List<Long>) {
        val pushSettings = pushSettingsProvider.getPushSettings()
        metaIds.containsAll(pushSettings.subscribedMetaAccounts)
        if (pushSettings.subscribedMetaAccounts.any { metaIds.contains(it) }) {
            val isPushEnabled = pushSettingsProvider.isPushNotificationsEnabled()
            updatePushSettings(isPushEnabled, pushSettings)
        }
    }

    override suspend fun onMetaAccountRemoved(metaId: Long) {
        val notificationsEnabled = pushSettingsProvider.isPushNotificationsEnabled()
        val noAccounts = accountRepository.getActiveMetaAccountsQuantity() == 0
        val pushSettings = pushSettingsProvider.getPushSettings()

        if (notificationsEnabled && noAccounts) {
            pushNotificationsService.updatePushSettings(false, null)
        } else if (noAccounts) {
            pushSettingsProvider.updateSettings(null)
        } else if (pushSettings.subscribedMetaAccounts.contains(metaId)) {
            val newPushSettings = pushSettings.copy(subscribedMetaAccounts = pushSettings.subscribedMetaAccounts - metaId)
            pushNotificationsService.updatePushSettings(notificationsEnabled, newPushSettings)
        }
    }
}
