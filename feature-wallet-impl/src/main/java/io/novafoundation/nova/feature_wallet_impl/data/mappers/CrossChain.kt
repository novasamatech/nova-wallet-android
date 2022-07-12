package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_wallet_api.domain.implementations.toInterior
import io.novafoundation.nova.feature_wallet_api.domain.model.AssetLocationPath
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.ReserveLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmDestination
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFeeAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import io.novafoundation.nova.feature_wallet_api.domain.model.XCMInstructionType
import io.novafoundation.nova.feature_wallet_api.domain.model.XcmTransferType
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.AssetLocationPathRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainOriginAssetRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.JunctionsRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.ReserveLocationRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmDestinationRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmFeeRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmTransferRemote
import java.math.BigInteger

fun mapCrossChainConfigFromRemote(remote: CrossChainTransfersConfigRemote): CrossChainTransfersConfiguration {
    val assetsLocations = remote.assetsLocation.mapValues { (_, reserveLocationRemote) ->
        mapReserveLocationFromRemote(reserveLocationRemote)
    }

    val feeInstructions = remote.instructions.mapValues { (_, instructionsRemote) ->
        instructionsRemote.map(::mapXcmInstructionFromRemote)
    }

    val chains = remote.chains.associateBy(
        keySelector = { it.chainId },
        valueTransform = { it.assets.map(::mapAssetTransfersFromRemote) }
    )

    return CrossChainTransfersConfiguration(
        assetLocations = assetsLocations,
        feeInstructions = feeInstructions,
        instructionBaseWeights = remote.networkBaseWeight,
        chains = chains
    )
}

private fun mapReserveLocationFromRemote(reserveLocationRemote: ReserveLocationRemote): ReserveLocation {
    return ReserveLocation(
        chainId = reserveLocationRemote.chainId,
        reserveFee = reserveLocationRemote.reserveFee?.let(::mapXcmFeeFromRemote),
        multiLocation = mapJunctionsRemoteToMultiLocation(reserveLocationRemote.multiLocation)
    )
}

private fun mapAssetTransfersFromRemote(remote: CrossChainOriginAssetRemote): AssetTransfers {
    return AssetTransfers(
        assetId = remote.assetId,
        assetLocationPath = mapLocationPathFromRemote(remote.assetLocationPath),
        assetLocation = remote.assetLocation,
        xcmTransfers = remote.xcmTransfers.map(::mapXcmTransferFromRemote)
    )
}

private fun mapLocationPathFromRemote(remote: AssetLocationPathRemote): AssetLocationPath {
    return when (remote.type) {
        "absolute" -> AssetLocationPath.Absolute
        "relative" -> AssetLocationPath.Relative
        "concrete" -> {
            val junctionsRemote = remote.path!!

            AssetLocationPath.Concrete(mapJunctionsRemoteToMultiLocation(junctionsRemote))
        }
        else -> AssetLocationPath.Unknown
    }
}

private fun mapXcmTransferFromRemote(remote: XcmTransferRemote): XcmTransfer {
    return XcmTransfer(
        destination = mapXcmDestinationFromRemote(remote.destination),
        type = mapXcmTransferTypeFromRemote(remote.type)
    )
}

private fun mapXcmTransferTypeFromRemote(remote: String): XcmTransferType {
    return when (remote) {
        "xtokens" -> XcmTransferType.X_TOKENS
        "xcmpallet" -> XcmTransferType.XCM_PALLET_RESERVE
        "xcmpallet-teleport" -> XcmTransferType.XCM_PALLET_TELEPORT
        else -> XcmTransferType.UNKNOWN
    }
}

private fun mapXcmDestinationFromRemote(remote: XcmDestinationRemote): XcmDestination {
    return XcmDestination(
        chainId = remote.chainId,
        assetId = remote.assetId,
        fee = mapXcmFeeFromRemote(remote.fee)
    )
}

private const val PARENTS = "parents"

private fun mapXcmFeeFromRemote(
    remote: XcmFeeRemote
): XcmFee<String> {
    val mode = when (remote.mode.type) {
        "proportional" -> XcmFee.Mode.Proportional(remote.mode.value.asGsonParsedNumber())
        "standard" -> XcmFee.Mode.Standard
        else -> XcmFee.Mode.Unknown
    }

    return XcmFee(
        mode = mode,
        instructions = remote.instructions,
        asset = remote.asset?.let {
            XcmFeeAsset(
                originAssetId = it.originAssetId,
                destAssetId = it.destAssetId,
                location = it.location,
                locationPath = mapLocationPathFromRemote(it.locationPath)
            )
        }
    )
}

private fun mapJunctionsRemoteToMultiLocation(
    junctionsRemote: JunctionsRemote
): MultiLocation {
    return if (PARENTS in junctionsRemote) {
        val parents = junctionsRemote.getValue(PARENTS).asGsonParsedNumber()
        val withoutParents = junctionsRemote - PARENTS

        MultiLocation(
            parents = parents,
            interior = mapJunctionsRemoteToInterior(withoutParents)
        )
    } else {
        MultiLocation(
            parents = BigInteger.ZERO,
            interior = mapJunctionsRemoteToInterior(junctionsRemote)
        )
    }
}

private fun mapXcmInstructionFromRemote(instruction: String): XCMInstructionType = runCatching {
    enumValueOf<XCMInstructionType>(instruction)
}.getOrDefault(XCMInstructionType.UNKNOWN)

private fun mapJunctionsRemoteToInterior(
    junctionsRemote: JunctionsRemote
): MultiLocation.Interior {
    return junctionsRemote.map { (type, value) -> mapJunctionFromRemote(type, value) }
        .toInterior()
}

private fun mapJunctionFromRemote(type: String, value: Any?): Junction {
    return when (type) {
        "parachainId" -> Junction.ParachainId(value.asGsonParsedNumber())
        "generalKey" -> Junction.GeneralKey(value as String)
        "palletInstance" -> Junction.PalletInstance(value.asGsonParsedNumber())
        "generalIndex" -> Junction.GeneralIndex(value.asGsonParsedNumber())
        else -> throw IllegalArgumentException("Unknown junction type: $type")
    }
}
