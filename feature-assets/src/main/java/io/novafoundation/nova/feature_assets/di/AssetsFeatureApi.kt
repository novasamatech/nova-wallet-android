package io.novafoundation.nova.feature_assets.di

import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry

interface AssetsFeatureApi {

    fun provideBuyTokenRegistry(): BuyTokenRegistry
}
