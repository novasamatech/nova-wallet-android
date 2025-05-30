package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.net.Uri
import io.novafoundation.nova.common.data.storage.Preferences

private const val PREFS_PENDING_DEEP_LINK = "pending_deep_link"

class PendingDeepLinkProvider(
    private val preferences: Preferences
) {

    fun save(data: Uri) {
        preferences.putString(PREFS_PENDING_DEEP_LINK, data.toString())
    }

    fun get(): Uri? {
        val deepLink = preferences.getString(PREFS_PENDING_DEEP_LINK) ?: return null
        return Uri.parse(deepLink)
    }

    fun clear() {
        preferences.removeField(PREFS_PENDING_DEEP_LINK)
    }
}
