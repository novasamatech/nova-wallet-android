package io.novafoundation.nova.feature_assets.di

import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory

interface AssetsFeatureApi {

    val updateSystem: BalancesUpdateSystem

    val coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory
}
