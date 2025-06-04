package io.novafoundation.nova.feature_deep_linking.presentation.configuring

import android.net.Uri

interface DeepLinkConfigurator<T> {

    enum class Type {
        APP_LINK, DEEP_LINK
    }

    fun configure(payload: T, type: Type = Type.DEEP_LINK): Uri
}
