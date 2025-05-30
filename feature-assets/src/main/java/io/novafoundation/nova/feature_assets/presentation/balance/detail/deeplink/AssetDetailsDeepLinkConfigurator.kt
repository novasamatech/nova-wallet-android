package io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink

import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.buildLink

class AssetDetailsDeepLinkData(
    val accountAddress: String?,
    val chainId: String,
    val assetId: Int
)

class AssetDetailsDeepLinkConfigurator(
    private val resourceManager: ResourceManager
) : DeepLinkConfigurator<AssetDetailsDeepLinkData> {

    val deepLinkPrefix = "/open/asset"
    val addressParam = "address"
    val chainIdParam = "chainId"
    val assetIdParam = "assetId"

    override fun configure(payload: AssetDetailsDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        return buildLink(resourceManager, deepLinkPrefix, type)
            .appendNullableQueryParameter(addressParam, payload.accountAddress)
            .appendQueryParameter(chainIdParam, payload.chainId)
            .appendQueryParameter(assetIdParam, payload.assetId.toString())
            .build()
    }
}
