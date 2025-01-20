package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.SimpleEdge
import io.novafoundation.nova.common.utils.isAscending
import io.novafoundation.nova.feature_wallet_api.domain.model.AssetLocationPath
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_wallet_api.domain.model.XcmTransferType
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.order
import io.novafoundation.nova.feature_xcm_api.multiLocation.toInterior
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isParachain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger

fun RelativeMultiLocation.localView(): RelativeMultiLocation {
    return RelativeMultiLocation(
        parents = 0,
        interior = when (val interior = interior) {
            Interior.Here -> interior

            is Interior.Junctions -> interior.junctions.takeLastWhile { it !is Junction.ParachainId }
                .toInterior()
        }
    )
}

fun XcmFee.Mode.Proportional.weightToFee(weight: Weight): BigInteger {
    val pico = BigInteger.TEN.pow(12)

    // Weight is an amount of picoseconds operation suppose to execute
    return weight * unitsPerSecond / pico
}

operator fun RelativeMultiLocation.plus(suffix: RelativeMultiLocation): RelativeMultiLocation {
    require(suffix.parents == 0) {
        "Appending multi location that has parents is not supported"
    }

    val newJunctions = junctions + suffix.junctions
    require(newJunctions.isAscending(compareBy { it.order })) {
        "Cannot append this multi location due to conflicting junctions"
    }

    return RelativeMultiLocation(
        parents = parents,
        interior = newJunctions.toInterior()
    )
}

fun RelativeMultiLocation.childView() =
    RelativeMultiLocation(parents + 1, interior)

fun CrossChainTransfersConfiguration.availableOutDestinations(origin: Chain.Asset): List<FullChainAssetId> {
    val assetTransfers = outComingAssetTransfers(origin) ?: return emptyList()

    return assetTransfers.xcmTransfers
        .filter { it.type != XcmTransferType.UNKNOWN }
        .map { it.destination.fullDestinationAssetId }
}

fun CrossChainTransfersConfiguration.availableInDestinations(destination: Chain.Asset): List<FullChainAssetId> {
    val requiredDestinationId = destination.fullId

    return chains.flatMap { (originChainId, chainTransfers) ->
        chainTransfers.mapNotNull { originAssetTransfers ->
            val hasDestination = originAssetTransfers.xcmTransfers
                .any { it.type != XcmTransferType.UNKNOWN && it.destination.fullDestinationAssetId == requiredDestinationId }

            FullChainAssetId(originChainId, originAssetTransfers.assetId).takeIf { hasDestination }
        }
    }
}

fun CrossChainTransfersConfiguration.availableInDestinations(): List<Edge<FullChainAssetId>> {
    return chains.flatMap { (originChainId, chainTransfers) ->
        chainTransfers.flatMap { originAssetTransfers ->
            originAssetTransfers.xcmTransfers.mapNotNull {
                if (it.type == XcmTransferType.UNKNOWN) return@mapNotNull null

                val from = FullChainAssetId(originChainId, originAssetTransfers.assetId)
                val to = FullChainAssetId(it.destination.chainId, it.destination.assetId)

                SimpleEdge(from, to)
            }
        }
    }
}

fun ByteArray.accountIdToMultiLocation() = RelativeMultiLocation(
    parents = 0,
    interior = Junctions(
        when (size) {
            32 -> Junction.AccountId32(intoKey())
            20 -> Junction.AccountKey20(intoKey())
            else -> throw IllegalArgumentException("Unsupported account id length: $size")
        }
    )
)

fun CrossChainTransfersConfiguration.transferConfiguration(
    originChain: Chain,
    originAsset: Chain.Asset,
    destinationChain: Chain,
    destinationParaId: ParaId? // null in case destination is relaychain
): CrossChainTransferConfiguration? {
    val assetTransfers = outComingAssetTransfers(originAsset) ?: return null
    val destination = assetTransfers.xcmTransfers.find { it.destination.chainId == destinationChain.id } ?: return null

    val reserveAssetLocation = assetLocations.getValue(assetTransfers.assetLocation)
    val hasReserveFee = reserveAssetLocation.chainId !in setOf(originChain.id, destinationChain.id)
    val reserveFee = if (hasReserveFee) {
        // reserve fee must be present if there is at least one non-reserve transfer
        matchInstructions(reserveAssetLocation.reserveFee!!, originChain.id, reserveAssetLocation.chainId)
    } else {
        null
    }

    val destinationFee = matchInstructions(
        destination.destination.fee,
        if (hasReserveFee) reserveAssetLocation.chainId else originChain.id,
        destination.destination.chainId
    )

    return CrossChainTransferConfiguration(
        originChainId = originChain.id,
        assetLocation = originAssetLocationOf(assetTransfers),
        reserveChainLocation = reserveAssetLocation.multiLocation,
        destinationChainLocation = destinationLocation(originChain, destinationParaId),
        destinationFee = destinationFee,
        reserveFee = reserveFee,
        transferType = destination.type
    )
}

private fun CrossChainTransfersConfiguration.matchInstructions(
    xcmFee: XcmFee<String>,
    fromChainId: ChainId,
    toChainId: ChainId,
): CrossChainFeeConfiguration {
    return CrossChainFeeConfiguration(
        from = CrossChainFeeConfiguration.From(
            chainId = fromChainId,
            deliveryFeeConfiguration = deliveryFeeConfigurations[fromChainId],
        ),
        to = CrossChainFeeConfiguration.To(
            chainId = toChainId,
            instructionWeight = instructionBaseWeights.getValue(toChainId),
            xcmFeeType = XcmFee(
                mode = xcmFee.mode,
                instructions = feeInstructions.getValue(xcmFee.instructions),
            )
        )
    )
}

private fun destinationLocation(
    originChain: Chain,
    destinationParaId: ParaId?
) = when {
    // parachain -> parachain
    originChain.isParachain && destinationParaId != null -> SiblingParachain(destinationParaId)

    // parachain -> relaychain
    originChain.isParachain -> ParentChain()

    // relaychain -> parachain
    destinationParaId != null -> ChildParachain(destinationParaId)

    // relaychain -> relaychain ?
    else -> throw UnsupportedOperationException("Unsupported cross-chain transfer")
}

private fun ChildParachain(paraId: ParaId): RelativeMultiLocation {
    return RelativeMultiLocation(
        parents = 0,
        interior = listOf(Junction.ParachainId(paraId)).toInterior()
    )
}

private fun ParentChain(): RelativeMultiLocation {
    return RelativeMultiLocation(
        parents = 1,
        interior = Interior.Here
    )
}

private fun SiblingParachain(paraId: ParaId): RelativeMultiLocation {
    return RelativeMultiLocation(
        parents = 1,
        listOf(Junction.ParachainId(paraId)).toInterior()
    )
}

private fun CrossChainTransfersConfiguration.originAssetLocationOf(assetTransfers: CrossChainTransfersConfiguration.AssetTransfers): RelativeMultiLocation {
    return when (val path = assetTransfers.assetLocationPath) {
        is AssetLocationPath.Absolute -> assetLocations.getValue(assetTransfers.assetLocation).multiLocation.childView()
        is AssetLocationPath.Relative -> assetLocations.getValue(assetTransfers.assetLocation).multiLocation.localView()
        is AssetLocationPath.Concrete -> path.multiLocation
    }
}

private fun CrossChainTransfersConfiguration.outComingAssetTransfers(origin: Chain.Asset): CrossChainTransfersConfiguration.AssetTransfers? {
    return chains[origin.chainId]?.find { it.assetId == origin.id }
}

private val CrossChainTransfersConfiguration.XcmDestination.fullDestinationAssetId: FullChainAssetId
    get() = FullChainAssetId(chainId, assetId)
