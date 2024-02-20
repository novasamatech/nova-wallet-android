package io.novafoundation.nova.feature_push_notifications.data.domain.interactor

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

interface PushNotificationsInteractor {

    suspend fun syncSettings()


    fun pushNotificationsEnabledFlow(): Flow<Boolean>

    suspend fun setPushNotificationsEnabled(enable: Boolean): Result<Unit>

    suspend fun updatePushSettings(enable: Boolean, pushSettings: PushSettings): Result<Unit>

    suspend fun getPushSettings(): PushSettings

    fun isPushNotificationsEnabled(): Boolean
}

class RealPushNotificationsInteractor(
    private val pushNotificationsService: PushNotificationsService,
    private val pushSettingsProvider: PushSettingsProvider,
    private val chainRegistry: ChainRegistry
) : PushNotificationsInteractor {

    override suspend fun syncSettings() {
        // TODO: To handle case when user disable a permission in settings
    }

    override fun governanceChainsFlow(): Flow<List<Chain>> {
        return chainRegistry.currentChains
            .map { it.filter { it.governance.isNotEmpty() } }
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
}
