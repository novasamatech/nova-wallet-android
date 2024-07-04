package io.novafoundation.nova.feature_assets.di

import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.CoinGeckoLinkParser

interface AssetsFeatureApi {

    val updateSystem: BalancesUpdateSystem
}
