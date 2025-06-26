package io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink

import android.net.Uri
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.addParamIfNotNull

class AssetDetailsDeepLinkData(
    val accountAddress: String?,
    val chainId: String,
    val assetId: Int
)

class AssetDetailsDeepLinkConfigurator(
    private val linkBuilderFactory: LinkBuilderFactory
) : DeepLinkConfigurator<AssetDetailsDeepLinkData> {

    val action = "open"
    val screen = "asset"
    val deepLinkPrefix = "/$action/$screen"
    val addressParam = "address"
    val chainIdParam = "chainId"
    val assetIdParam = "assetId"

    override fun configure(payload: AssetDetailsDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        return linkBuilderFactory.newLink(type)
            .setAction(action)
            .setScreen(screen)
            .addParamIfNotNull(addressParam, payload.accountAddress)
            .addParam(chainIdParam, payload.chainId)
            .addParam(assetIdParam, payload.assetId.toString())
            .build()
    }
}
