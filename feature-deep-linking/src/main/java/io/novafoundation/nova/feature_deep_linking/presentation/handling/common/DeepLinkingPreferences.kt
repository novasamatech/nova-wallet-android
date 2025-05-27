package io.novafoundation.nova.feature_deep_linking.presentation.handling.common

import android.net.Uri

class DeepLinkingPreferences(
    val deepLinkScheme: String,
    val deepLinkHost: String,
    val appLinkHost: String,
    val branchIoLinkHosts: List<String>
)

fun Uri.isDeepLink(preferences: DeepLinkingPreferences): Boolean {
    return scheme == preferences.deepLinkScheme && host == preferences.deepLinkHost
}
