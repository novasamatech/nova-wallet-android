package io.novafoundation.nova.feature_assets.di

import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_assets.di.modules.deeplinks.AssetDeepLinks
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.MultisigRestrictionCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator

interface AssetsFeatureApi {

    val updateSystem: BalancesUpdateSystem

    val coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory

    val assetDeepLinks: AssetDeepLinks

    val assetDetailsDeepLinkConfigurator: AssetDetailsDeepLinkConfigurator

    val multisigRestrictionCheckMixin: MultisigRestrictionCheckMixin
}
