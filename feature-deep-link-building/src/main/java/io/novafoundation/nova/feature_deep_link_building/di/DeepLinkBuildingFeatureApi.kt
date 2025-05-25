package io.novafoundation.nova.feature_deep_link_building.di

import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsDeepLinkConfigurator

interface DeepLinkBuildingFeatureApi {

    val referendumDetailsDeepLinkConfigurator: io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsDeepLinkConfigurator

    val assetDetailsDeepLinkConfigurator: io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
}
