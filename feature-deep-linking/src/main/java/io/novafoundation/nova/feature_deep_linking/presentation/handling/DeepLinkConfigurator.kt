package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.content.Intent
import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_deep_linking.R

interface DeepLinkConfigurator<T> {

    fun configure(payload: T): Uri
}

fun buildDeepLink(resourceManager: ResourceManager, path: String): Uri.Builder {
    val scheme = resourceManager.getString(R.string.deep_linking_scheme)
    val host = resourceManager.getString(R.string.deep_linking_host)
    val handledPath = path.removePrefix("/")

    return Uri.Builder()
        .scheme(scheme)
        .authority(host)
        .path(handledPath)
}
