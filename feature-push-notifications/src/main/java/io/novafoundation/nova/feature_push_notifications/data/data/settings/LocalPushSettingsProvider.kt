package io.novafoundation.nova.feature_push_notifications.data.data.settings

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences

private const val PUSH_SETTINGS_KEY = "push_settings"

class LocalPushSettingsProvider(
    private val gson: Gson,
    private val prefs: Preferences
) : PushSettingsProvider {

    override suspend fun getWalletSettings(): PushWalletSettings? {
        return prefs.getString(PUSH_SETTINGS_KEY)
            ?.let { gson.fromJson(it, PushWalletSettings::class.java) }
    }

    override suspend fun updateWalletSettings(pushWalletSettings: PushWalletSettings) {
        prefs.putString(PUSH_SETTINGS_KEY, gson.toJson(pushWalletSettings))
    }
}
