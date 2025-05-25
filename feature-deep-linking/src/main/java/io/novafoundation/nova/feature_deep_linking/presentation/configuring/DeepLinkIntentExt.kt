package io.novafoundation.nova.feature_deep_linking.presentation.configuring

import android.content.Intent


fun <T> Intent.applyDeepLink(configurator: DeepLinkConfigurator<T>, payload: T): Intent {
    data = configurator.configure(payload)
    return this
}
