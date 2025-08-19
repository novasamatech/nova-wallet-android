package io.novafoundation.nova.feature_push_notifications.data.settings

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_push_notifications.data.settings.model.PushSettingsCacheV1
import io.novafoundation.nova.feature_push_notifications.data.settings.model.PushSettingsCacheV2
import io.novafoundation.nova.feature_push_notifications.data.settings.model.VersionedPushSettingsCache
import io.novafoundation.nova.feature_push_notifications.data.settings.model.toCache
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import kotlinx.coroutines.flow.Flow

private const val PUSH_SETTINGS_KEY = "push_settings"
private const val PREFS_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"

class RealPushSettingsProvider(
    private val gson: Gson,
    private val prefs: Preferences,
    private val accountRepository: AccountRepository
) : PushSettingsProvider {

    override suspend fun getPushSettings(): PushSettings {
        return prefs.getString(PUSH_SETTINGS_KEY)
            ?.let {
                gson.fromJson(it, VersionedPushSettingsCache::class.java)
                    .toPushSettings()
            } ?: getDefaultPushSettings()
    }

    override suspend fun getDefaultPushSettings(): PushSettings {
        return PushSettings(
            announcementsEnabled = true,
            sentTokensEnabled = true,
            receivedTokensEnabled = true,
            subscribedMetaAccounts = setOf(accountRepository.getSelectedMetaAccount().id),
            stakingReward = PushSettings.ChainFeature.All,
            governance = emptyMap(),
            multisigs = PushSettings.MultisigsState.disabled()
        )
    }

    override fun updateSettings(pushWalletSettings: PushSettings?) {
        val versionedCache = pushWalletSettings?.toCache()
            ?.toVersionedPushSettingsCache()

        prefs.putString(PUSH_SETTINGS_KEY, versionedCache?.let(gson::toJson))
    }

    override fun setPushNotificationsEnabled(isEnabled: Boolean) {
        prefs.putBoolean(PREFS_PUSH_NOTIFICATIONS_ENABLED, isEnabled)
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return prefs.getBoolean(PREFS_PUSH_NOTIFICATIONS_ENABLED, false)
    }

    override fun pushEnabledFlow(): Flow<Boolean> {
        return prefs.booleanFlow(PREFS_PUSH_NOTIFICATIONS_ENABLED, false)
    }

    fun PushSettingsCacheV2.toVersionedPushSettingsCache(): VersionedPushSettingsCache {
        return VersionedPushSettingsCache(
            version = version,
            settings = gson.toJson(this)
        )
    }

    fun VersionedPushSettingsCache.toPushSettings(): PushSettings {
        return when (version) {
            PushSettingsCacheV1.VERSION -> gson.fromJson(settings, PushSettingsCacheV1::class.java)
            PushSettingsCacheV2.VERSION -> gson.fromJson(settings, PushSettingsCacheV2::class.java)
            else -> throw IllegalStateException("Unknown push settings version: $version")
        }.toPushSettings()
    }
}
