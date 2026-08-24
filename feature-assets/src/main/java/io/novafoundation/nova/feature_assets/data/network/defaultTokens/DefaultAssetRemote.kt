package io.novafoundation.nova.feature_assets.data.network.defaultTokens

import com.google.gson.annotations.SerializedName

class DefaultAssetRemote(
    @SerializedName("chainId") val chainId: String,
    @SerializedName("assetId") val assetId: Int
)
