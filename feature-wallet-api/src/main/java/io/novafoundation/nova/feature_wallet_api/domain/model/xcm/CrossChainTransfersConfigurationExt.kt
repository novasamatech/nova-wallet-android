package io.novafoundation.nova.feature_wallet_api.domain.model.xcm

import android.util.Log
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.availableOutDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.hasDeliveryFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.transferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.availableOutDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.hasDeliveryFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.transferConfiguration
import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

fun CrossChainTransfersConfiguration.availableOutDestinations(origin: Chain.Asset): List<FullChainAssetId> {
    val combined = dynamic.availableOutDestinations(origin) + legacy.availableOutDestinations(origin)
    return combined.distinct()
}

fun CrossChainTransfersConfiguration.availableInDestinations(destination: Chain.Asset): List<FullChainAssetId> {
    val combined = dynamic.availableInDestinations(destination) + legacy.availableInDestinations(destination)
    return combined.distinct()
}

fun CrossChainTransfersConfiguration.availableInDestinations(): List<Edge<FullChainAssetId>> {
    val combined = dynamic.availableInDestinations() + legacy.availableInDestinations()
    return combined.distinct()
}

fun CrossChainTransfersConfiguration.hasDeliveryFee(
    origin: FullChainAssetId,
    destination: FullChainAssetId
): Boolean {
    return dynamic.hasDeliveryFee(origin, destination) ?: legacy.hasDeliveryFee(origin.chainId)
}

suspend fun CrossChainTransfersConfiguration.transferConfiguration(
    originChain: XcmChain,
    originAsset: Chain.Asset,
    destinationChain: XcmChain,
): CrossChainTransferConfiguration? {
    val result = dynamic.transferConfiguration(originChain, originAsset, destinationChain)?.let(CrossChainTransferConfiguration::Dynamic)
        ?: legacy.transferConfiguration(originChain, originAsset, destinationChain)?.let(CrossChainTransferConfiguration::Legacy)

    logTransferConfiguration(originAsset, originChain, destinationChain, result)

    return result
}

private fun logTransferConfiguration(
    originAsset: Chain.Asset,
    originChain: XcmChain,
    destinationChain: XcmChain,
    result: CrossChainTransferConfiguration?
) {
    val logDirectionLabel = "${originAsset.symbol} ${originChain.chain.name} -> ${destinationChain.chain.name}"
    if (result == null) {
        Log.d("CrossChainTransfersConfiguration", "Found no configuration for direction $logDirectionLabel")
    } else {
        Log.d("CrossChainTransfersConfiguration", "Using ${result::class.simpleName} configuration for direction $logDirectionLabel")
    }
}
