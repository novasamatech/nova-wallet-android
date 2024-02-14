package io.novafoundation.nova.feature_push_notifications.data.data.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.gson.SealedTypeAdapterFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PUSH_SETTINGS_KEY = "push_settings"
private const val PREFS_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"

class RealPushSettingsProvider(
    private val gson: Gson,
    private val prefs: Preferences,
    private val accountRepository: AccountRepository
) : PushSettingsProvider {

    override suspend fun getPushSettings(): PushSettings {
        return prefs.getString(PUSH_SETTINGS_KEY)
            ?.let { gson.fromJson(it, PushSettings::class.java) }
            ?: getDefaultPushSettings()
    }

    override suspend fun getDefaultPushSettings(): PushSettings {
        return PushSettings.getDefault(listOf(accountRepository.getSelectedMetaAccount().toWalletSettings()))
    }

    override fun updateWalletSettings(pushWalletSettings: PushSettings) {
        prefs.putString(PUSH_SETTINGS_KEY, gson.toJson(pushWalletSettings))
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
}
