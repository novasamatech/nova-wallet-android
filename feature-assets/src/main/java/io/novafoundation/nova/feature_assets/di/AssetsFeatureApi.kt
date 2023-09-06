package io.novafoundation.nova.feature_assets.di

import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem

interface AssetsFeatureApi {

    val buyTokenRegistry: BuyTokenRegistry

    val updateSystem: BalancesUpdateSystem
}
