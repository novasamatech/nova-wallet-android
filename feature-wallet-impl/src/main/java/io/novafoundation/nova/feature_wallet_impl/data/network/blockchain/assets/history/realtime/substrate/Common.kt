package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.feature_xcm_api.multiLocation.bindMultiLocation
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.assetTxFeePaidEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

suspend fun List<GenericEvent.Instance>.assetFee(multiLocationConverter: MultiLocationConverter): ChainAssetWithAmount? {
    val event = assetTxFeePaidEvent() ?: return null
    val (_, actualFee, tip, assetId) = event.arguments
    val totalFee = bindNumber(actualFee) + bindNumber(tip)
    val chainAsset = multiLocationConverter.toChainAsset(bindMultiLocation(assetId)) ?: return null

    return ChainAssetWithAmount(chainAsset, totalFee)
}
