package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent

suspend fun ExtrinsicWithEvents.assetFee(multiLocationConverter: MultiLocationConverter): ChainAssetWithAmount? {
    val event = findEvent(Modules.ASSET_TX_PAYMENT, "AssetTxFeePaid") ?: return null
    val (_, actualFee, tip, assetId) = event.arguments
    val totalFee = bindNumber(actualFee) + bindNumber(tip)
    val chainAsset = multiLocationConverter.toChainAsset(bindMultiLocation(assetId)) ?: return null

    return ChainAssetWithAmount(chainAsset, totalFee)
}
