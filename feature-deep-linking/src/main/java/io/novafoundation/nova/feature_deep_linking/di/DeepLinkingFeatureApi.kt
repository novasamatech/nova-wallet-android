package io.novafoundation.nova.feature_deep_linking.di

import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler

interface DeepLinkingFeatureApi {

    val rootDeepLinkHandler: RootDeepLinkHandler

    val referendumDeepLinkHandler: ReferendumDeepLinkHandler

    val assetDetailsDeepLinkHandler: AssetDetailsDeepLinkHandler
}
