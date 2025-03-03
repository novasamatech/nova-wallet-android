package io.novafoundation.nova.feature_deep_link_building.presentation

import android.content.Intent
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

fun Intent.addReferendumData(
    deepLinkConfigurator: DeepLinkConfigurator<ReferendumDeepLinkData>,
    chainId: String,
    referendumId: BigInteger
): Intent {
    val payload = ReferendumDeepLinkData(chainId, referendumId, Chain.Governance.V2)
    data = deepLinkConfigurator.configure(payload)
    return this
}

fun Intent.addAssetDetailsData(
    deepLinkConfigurator: DeepLinkConfigurator<AssetDetailsDeepLinkData>,
    address: String,
    chainId: String,
    assetId: Int
): Intent {
    val payload = AssetDetailsDeepLinkData(address, chainId, assetId)
    data = deepLinkConfigurator.configure(payload)
    return this
}
