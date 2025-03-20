package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.dryRun

import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.XcmAssetIssuer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AssetIssuerRegistry : XcmAssetIssuer {

    suspend fun create(chainAsset: Chain.Asset): AssetIssuer
}
