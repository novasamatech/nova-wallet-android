package io.novafoundation.nova.feature_deep_link_building.presentation

import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_deep_link_building.R

interface DeepLinkConfigurator<T> {

    enum class Type {
        APP_LINK, DEEP_LINK
    }

    fun configure(payload: T, type: Type = Type.DEEP_LINK): Uri
}

fun buildLink(resourceManager: ResourceManager, path: String, type: DeepLinkConfigurator.Type): Uri.Builder {
    return when (type) {
        DeepLinkConfigurator.Type.APP_LINK -> buildAppLink(resourceManager, path)
        DeepLinkConfigurator.Type.DEEP_LINK -> buildDeepLink(resourceManager, path)
    }
}

private fun buildAppLink(resourceManager: ResourceManager, path: String): Uri.Builder {
    val scheme = "https"
    val host = resourceManager.getString(R.string.app_link_scheme)
    val handledPath = path.removePrefix("/")

    return Uri.Builder()
        .scheme(scheme)
        .authority(host)
        .path(handledPath)
}

private fun buildDeepLink(resourceManager: ResourceManager, path: String): Uri.Builder {
    val scheme = resourceManager.getString(R.string.deep_linking_scheme)
    val host = resourceManager.getString(R.string.deep_linking_host)
    val handledPath = path.removePrefix("/")

    return Uri.Builder()
        .scheme(scheme)
        .authority(host)
        .path(handledPath)
}
