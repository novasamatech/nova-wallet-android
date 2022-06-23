package io.novafoundation.nova.feature_wallet_impl.domain.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.isAscending
import io.novafoundation.nova.feature_wallet_api.domain.model.AssetLocationPath
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import io.novafoundation.nova.feature_wallet_api.domain.model.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.order
import io.novafoundation.nova.runtime.ext.isParachain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

fun MultiLocation.localView(): MultiLocation {
    return MultiLocation(
        parents = BigInteger.ZERO,
        interior = when (val interior = interior) {
            MultiLocation.Interior.Here -> interior

            is MultiLocation.Interior.Junctions -> interior.junctions.takeLastWhile { it !is Junction.ParachainId }
                .toInterior()
        }
    )
}

fun XcmFee.Mode.Proportional.weightToFee(weight: Weight): BigInteger {
    val pico = BigInteger.TEN.pow(12)

    // Weight is an amount of picoseconds operation suppose to execute
    return weight * unitsPerSecond / pico
}

operator fun MultiLocation.plus(suffix: MultiLocation): MultiLocation {
    require(suffix.parents == BigInteger.ZERO) {
        "Appending multi location that has parents is not supported"
    }

    val newJunctions = interior.junctionList + suffix.interior.junctionList
    require(newJunctions.isAscending(compareBy { it.order })) {
        "Cannot append this multi location due to conflicting junctions"
    }

    return MultiLocation(
        parents = parents,
        interior = newJunctions.toInterior()
    )
}

private val MultiLocation.Interior.junctionList: List<Junction>
    get() = when (this) {
        MultiLocation.Interior.Here -> emptyList()
        is MultiLocation.Interior.Junctions -> junctions
    }

fun List<Junction>.toInterior() = when (size) {
    0 -> MultiLocation.Interior.Here
    else -> MultiLocation.Interior.Junctions(this)
}

fun MultiLocation.childView() = MultiLocation(parents + BigInteger.ONE, interior)

fun CrossChainTransfersConfiguration.availableDestinationChains(origin: Chain.Asset): List<ChainId> {
    val assetTransfers = assetTransfers(origin) ?: return emptyList()

    return assetTransfers.xcmTransfers
        .filter { it.type != XcmTransferType.UNKNOWN }
        .map { it.destination.chainId }
}

fun CrossChainTransfersConfiguration.transferConfiguration(
    originChain: Chain,
    originAsset: Chain.Asset,
    destinationChain: Chain,
    destinationParaId: ParaId? // null in case destination is relaychain
): CrossChainTransferConfiguration? {
    val assetTransfers = assetTransfers(originAsset) ?: return null
    val destination = assetTransfers.xcmTransfers.find { it.destination.chainId == destinationChain.id } ?: return null

    val reserveAssetLocation = assetLocations.getValue(assetTransfers.assetLocation)
    val hasReserveFee = reserveAssetLocation.chainId !in setOf(originChain.id, destinationChain.id)
    val reserveFee = if (hasReserveFee) {
        // reserve fee must be present if there is at least one non-reserve transfer
        matchInstructions(reserveAssetLocation.reserveFee!!, reserveAssetLocation.chainId)
    } else {
        null
    }

    return CrossChainTransferConfiguration(
        assetLocation = originAssetLocationOf(assetTransfers),
        destinationChainLocation = destinationLocation(originChain, destinationParaId),
        destinationFee = matchInstructions(destination.destination.fee, destination.destination.chainId),
        reserveFee = reserveFee,
        transferType = destination.type
    )
}

private fun CrossChainTransfersConfiguration.matchInstructions(
    xcmFee: XcmFee<String>,
    chainId: ChainId,
): CrossChainFeeConfiguration {
    return CrossChainFeeConfiguration(
        chainId = chainId,
        instructionWeight = instructionBaseWeights.getValue(chainId),
        xcmFeeType = XcmFee(
            mode = xcmFee.mode,
            instructions = feeInstructions.getValue(xcmFee.instructions),
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

private fun ChildParachain(paraId: ParaId): MultiLocation {
    return MultiLocation(
        parents = BigInteger.ZERO,
        interior = listOf(Junction.ParachainId(paraId)).toInterior()
    )
}

private fun ParentChain(): MultiLocation {
    return MultiLocation(
        parents = BigInteger.ONE,
        interior = MultiLocation.Interior.Here
    )
}

private fun SiblingParachain(paraId: ParaId): MultiLocation {
    return MultiLocation(
        parents = BigInteger.ONE,
        listOf(Junction.ParachainId(paraId)).toInterior()
    )
}

private fun CrossChainTransfersConfiguration.originAssetLocationOf(assetTransfers: CrossChainTransfersConfiguration.AssetTransfers): MultiLocation {
    return when (val path = assetTransfers.assetLocationPath) {
        is AssetLocationPath.Absolute -> assetLocations.getValue(assetTransfers.assetLocation).multiLocation.childView()
        is AssetLocationPath.Relative -> assetLocations.getValue(assetTransfers.assetLocation).multiLocation.localView()
        is AssetLocationPath.Concrete -> path.multiLocation
    }
}

private fun CrossChainTransfersConfiguration.assetTransfers(origin: Chain.Asset): CrossChainTransfersConfiguration.AssetTransfers? {
    return chains[origin.chainId]?.find { it.assetId == origin.id }
}
