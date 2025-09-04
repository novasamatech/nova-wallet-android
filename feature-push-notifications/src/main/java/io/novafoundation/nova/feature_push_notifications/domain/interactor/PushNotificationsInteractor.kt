package io.novafoundation.nova.feature_push_notifications.domain.interactor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsAvailabilityState
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.repository.PushSettingsRepository
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import kotlinx.coroutines.flow.Flow

interface PushNotificationsInteractor {

    suspend fun initialSyncSettings()

    fun pushNotificationsEnabledFlow(): Flow<Boolean>

    suspend fun initPushSettings(): Result<Unit>

    suspend fun updatePushSettings(enable: Boolean, pushSettings: PushSettings): Result<Unit>

    suspend fun getPushSettings(): PushSettings

    suspend fun getDefaultSettings(): PushSettings

    suspend fun getMetaAccounts(metaIds: List<Long>): List<MetaAccount>

    fun isPushNotificationsEnabled(): Boolean

    fun isMultisigsWasEnabledFirstTime(): Boolean

    fun setMultisigsWasEnabledFirstTime()

    fun pushNotificationsAvailabilityState(): PushNotificationsAvailabilityState

    fun isPushNotificationsAvailable(): Boolean

    suspend fun onMetaAccountChange(changed: List<Long>, deleted: List<Long>)

    suspend fun filterAvailableMetaIdsAndGetNewState(pushSettings: PushSettings): PushSettings

    suspend fun getNewStateForChangedMetaAccounts(currentSettings: PushSettings, newMetaIds: Set<Long>): PushSettings
}

class RealPushNotificationsInteractor(
    private val pushNotificationsService: PushNotificationsService,
    private val pushSettingsProvider: PushSettingsProvider,
    private val accountRepository: AccountRepository,
    private val pushSettingsRepository: PushSettingsRepository
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

    override suspend fun getMetaAccounts(metaIds: List<Long>): List<MetaAccount> {
        return accountRepository.getMetaAccountsByIds(metaIds)
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return pushSettingsProvider.isPushNotificationsEnabled()
    }

    override fun isMultisigsWasEnabledFirstTime(): Boolean {
        return pushSettingsRepository.isMultisigsWasEnabledFirstTime()
    }

    override fun setMultisigsWasEnabledFirstTime() {
        pushSettingsRepository.setMultisigsWasEnabledFirstTime()
    }

    override fun isPushNotificationsAvailable(): Boolean {
        return pushNotificationsService.isPushNotificationsAvailable()
    }

    override fun pushNotificationsAvailabilityState(): PushNotificationsAvailabilityState {
        return pushNotificationsService.pushNotificationsAvaiabilityState()
    }

    override suspend fun onMetaAccountChange(changed: List<Long>, deleted: List<Long>) {
        if (changed.isEmpty() && deleted.isEmpty()) return

        val notificationsEnabled = pushSettingsProvider.isPushNotificationsEnabled()
        val noAccounts = accountRepository.getActiveMetaAccountsQuantity() == 0
        val pushSettings = pushSettingsProvider.getPushSettings()

        val allAffected = (changed + deleted).toSet()
        val subscribedAccountsAffected = pushSettings.subscribedMetaAccounts.intersect(allAffected).isNotEmpty()

        when {
            notificationsEnabled && noAccounts -> pushNotificationsService.updatePushSettings(enabled = false, pushSettings = null)
            noAccounts -> pushSettingsProvider.updateSettings(pushWalletSettings = null)
            subscribedAccountsAffected -> {
                val newSubscribedMetaAccounts = pushSettings.subscribedMetaAccounts - deleted.toSet()
                val newEnabledState = notificationsEnabled && newSubscribedMetaAccounts.isNotEmpty()
                val newPushSettings = getNewStateForChangedMetaAccounts(pushSettings, newSubscribedMetaAccounts)
                pushNotificationsService.updatePushSettings(enabled = newEnabledState, pushSettings = newPushSettings)
            }
        }
    }

    override suspend fun filterAvailableMetaIdsAndGetNewState(pushSettings: PushSettings): PushSettings {
        val availableMetaIds = accountRepository.getAvailableMetaIdsFromSet(pushSettings.subscribedMetaAccounts)
        if (availableMetaIds == pushSettings.subscribedMetaAccounts) return pushSettings

        return getNewStateForChangedMetaAccounts(pushSettings, availableMetaIds)
    }

    override suspend fun getNewStateForChangedMetaAccounts(currentSettings: PushSettings, newMetaIds: Set<Long>): PushSettings {
        val noMultisigWalletsForNewAccounts = !accountRepository.hasMetaAccountsByType(newMetaIds, LightMetaAccount.Type.MULTISIG)
        if (noMultisigWalletsForNewAccounts) {
            val disabledMultisigSettings = PushSettings.MultisigsState.disabled()
            return currentSettings.copy(subscribedMetaAccounts = newMetaIds, multisigs = disabledMultisigSettings)
        }

        return if (!currentSettings.multisigs.isEnabled) {
            currentSettings.copy(subscribedMetaAccounts = newMetaIds, multisigs = PushSettings.MultisigsState.enabled())
        } else {
            currentSettings.copy(subscribedMetaAccounts = newMetaIds)
        }
    }
}
