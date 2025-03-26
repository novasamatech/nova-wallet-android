package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.SimpleEdge
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration.AssetTransfers
import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

fun DynamicCrossChainTransfersConfiguration.availableOutDestinations(origin: Chain.Asset): List<FullChainAssetId> {
    val assetTransfers = outComingAssetTransfers(origin.fullId) ?: return emptyList()
    return assetTransfers.destinations.map { it.fullChainAssetId }
}

fun DynamicCrossChainTransfersConfiguration.availableInDestinations(destination: Chain.Asset): List<FullChainAssetId> {
    val requiredDestinationId = destination.fullId

    return chains.flatMap { (originChainId, chainTransfers) ->
        chainTransfers.mapNotNull { originAssetTransfers ->
            val hasDestination = originAssetTransfers.destinations.any { it.fullChainAssetId == requiredDestinationId }

            if (hasDestination) {
                FullChainAssetId(originChainId, originAssetTransfers.assetId)
            } else {
                null
            }
        }
    }
}

fun DynamicCrossChainTransfersConfiguration.availableInDestinations(): List<Edge<FullChainAssetId>> {
    return chains.flatMap { (originChainId, chainTransfers) ->
        chainTransfers.flatMap { originAssetTransfers ->
            originAssetTransfers.destinations.map {
                val from = FullChainAssetId(originChainId, originAssetTransfers.assetId)
                val to = it.fullChainAssetId

                SimpleEdge(from, to)
            }
        }
    }
}

fun DynamicCrossChainTransfersConfiguration.transferConfiguration(
    originXcmChain: XcmChain,
    originAsset: Chain.Asset,
    destinationXcmChain: XcmChain,
): DynamicCrossChainTransferConfiguration? {
    val destinationChain = destinationXcmChain.chain

    val assetTransfers = outComingAssetTransfers(originAsset.fullId) ?: return null
    val targetTransfer = assetTransfers.destinations.find { it.fullChainAssetId.chainId == destinationChain.id } ?: return null

    val reserve = reserveRegistry.getReserve(originAsset)

    return DynamicCrossChainTransferConfiguration(
        originChain = originXcmChain,
        destinationChain = destinationXcmChain,
        reserve = reserve,
        hasDeliveryFee = targetTransfer.hasDeliveryFee
    )
}

/**
 * @return null if transfer is unknown, true if delivery fee has to be paid, false otherwise
 */
fun DynamicCrossChainTransfersConfiguration.hasDeliveryFee(
    origin: FullChainAssetId,
    destination: FullChainAssetId
): Boolean? {
    val transfers = outComingAssetTransfers(origin) ?: return null
    val destinationConfig = transfers.destinations.find { it.fullChainAssetId == destination } ?: return null

    return destinationConfig.hasDeliveryFee
}

private fun DynamicCrossChainTransfersConfiguration.outComingAssetTransfers(origin: FullChainAssetId): AssetTransfers? {
    return chains[origin.chainId]?.find { it.assetId == origin.assetId }
}
