package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.isAscending
import io.novafoundation.nova.feature_wallet_api.domain.model.AssetLocationPath
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_wallet_api.domain.model.Junctions
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import io.novafoundation.nova.feature_wallet_api.domain.model.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.order
import io.novafoundation.nova.runtime.ext.isParachain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
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

fun CrossChainTransfersConfiguration.availableDestinations(origin: Chain.Asset): List<Pair<ChainId, ChainAssetId>> {
    val assetTransfers = assetTransfers(origin) ?: return emptyList()

    return assetTransfers.xcmTransfers
        .filter { it.type != XcmTransferType.UNKNOWN }
        .map { it.destination.chainId to it.destination.assetId }
}

fun ByteArray.accountIdToMultiLocation() = MultiLocation(
    parents = BigInteger.ZERO,
    interior = Junctions(
        when (size) {
            32 -> Junction.AccountId32(this)
            20 -> Junction.AccountKey20(this)
            else -> throw IllegalArgumentException("Unsupported account id length: $size")
        }
    )
)

fun CrossChainTransfersConfiguration.crossChainFeeAssetId(
    originChainId: ChainId,
    originAssetId: ChainAssetId,
    destinationChainId: ChainId,
): Chain.Asset.FullId? {
    val assetTransfers = assetTransfers(originChainId, originAssetId) ?: return null
    val destination = assetTransfers.findDestination(destinationChainId) ?: return null

    // default to used asset for paying cross-chain fee
    val assetId = destination.destination.fee.asset?.originAssetId ?: originAssetId

    return Chain.Asset.FullId(
        chainId = originChainId,
        assetId = assetId
    )
}

fun CrossChainTransfersConfiguration.transferConfiguration(
    originChain: Chain,
    originAsset: Chain.Asset,
    destinationChain: Chain,
    destinationParaId: ParaId? // null in case destination is relaychain
): CrossChainTransferConfiguration? {
    val assetTransfers = assetTransfers(originAsset) ?: return null
    val destination = assetTransfers.findDestination(destinationChain.id) ?: return null

    val customFeeConfiguration = destination.destination.fee.asset
    val customFeeAssetLocation = customFeeConfiguration?.let {
        assetLocationOf(it.locationPath, it.location)
    }

    val feeReserveLocationId = customFeeConfiguration?.location ?: assetTransfers.assetLocation
    val feeReserveLocation = assetLocations.getValue(feeReserveLocationId)

    val hasReserveFee = feeReserveLocation.chainId !in setOf(originChain.id, destinationChain.id)

    val reserveFee = if (hasReserveFee) {
        // reserve fee must be present if there is at least one non-reserve transfer
        matchInstructions(feeReserveLocation.reserveFee!!, feeReserveLocation.chainId)
    } else {
        null
    }

    return CrossChainTransferConfiguration(
        assetLocation = originAssetLocationOf(assetTransfers),
        customFeeAssetLocation = customFeeAssetLocation,
        destinationChainLocation = destinationLocation(originChain, destinationParaId),
        destinationFee = matchInstructions(destination.destination.fee, destination.destination.chainId),
        reserveFee = reserveFee,
        transferType = destination.type
    )
}

private fun CrossChainTransfersConfiguration.AssetTransfers.findDestination(
    destinationChainId: ChainId
) = xcmTransfers.find { it.destination.chainId == destinationChainId }

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
            asset = xcmFee.asset,
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

private fun CrossChainTransfersConfiguration.assetLocationOf(
    assetLocationPath: AssetLocationPath,
    assetLocation: String
): MultiLocation {
    return when (assetLocationPath) {
        is AssetLocationPath.Absolute -> assetLocations.getValue(assetLocation).multiLocation.childView()
        is AssetLocationPath.Relative -> assetLocations.getValue(assetLocation).multiLocation.localView()
        is AssetLocationPath.Concrete -> assetLocationPath.multiLocation
        AssetLocationPath.Unknown -> throw java.lang.UnsupportedOperationException("Unknown location path type")
    }
}

private fun CrossChainTransfersConfiguration.originAssetLocationOf(assetTransfers: CrossChainTransfersConfiguration.AssetTransfers): MultiLocation {
    return assetLocationOf(assetTransfers.assetLocationPath, assetTransfers.assetLocation)
}

private fun CrossChainTransfersConfiguration.assetTransfers(origin: Chain.Asset): CrossChainTransfersConfiguration.AssetTransfers? {
    return assetTransfers(origin.chainId, origin.id)
}

private fun CrossChainTransfersConfiguration.assetTransfers(
    originChainId: ChainId,
    originAssetId: ChainAssetId,
): CrossChainTransfersConfiguration.AssetTransfers? {
    return chains[originChainId]?.find { it.assetId == originAssetId }
}
