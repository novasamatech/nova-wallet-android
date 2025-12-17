package io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain

import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveConfig
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.AssetLocationPath
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.DeliveryFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.ReserveLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmDestination
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.XCMInstructionType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyXcmTransferMethod
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyCrossChainOriginAssetRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyCrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyDeliveryFeeConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyNetworkDeliveryFeeRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyReserveLocationRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyXcmDestinationRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyXcmFeeRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyXcmTransferRemote
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

fun LegacyCrossChainTransfersConfigRemote.toDomain(
    parachainInfoRepository: ParachainInfoRepository
): LegacyCrossChainTransfersConfiguration {
    val assetsLocations = assetsLocation.orEmpty().mapValues { (_, reserveLocationRemote) ->
        mapReserveLocationFromRemote(reserveLocationRemote)
    }

    val feeInstructions = instructions.orEmpty().mapValues { (_, instructionsRemote) ->
        instructionsRemote.map(::mapXcmInstructionFromRemote)
    }

    val chains = chains.orEmpty().associateBy(
        keySelector = { it.chainId },
        valueTransform = { it.assets.map(::mapAssetTransfersFromRemote) }
    )

    val networkDeliveryFee = networkDeliveryFee.orEmpty().mapValues { (_, networkDeliveryFeeRemote) ->
        mapNetworkDeliveryFeeFromRemote(networkDeliveryFeeRemote)
    }

    return LegacyCrossChainTransfersConfiguration(
        assetLocations = assetsLocations,
        feeInstructions = feeInstructions,
        instructionBaseWeights = networkBaseWeight.orEmpty(),
        deliveryFeeConfigurations = networkDeliveryFee,
        chains = chains,
        reserveRegistry = constructLegacyReserveRegistry(parachainInfoRepository, assetsLocations, chains)
    )
}

private fun constructLegacyReserveRegistry(
    parachainInfoRepository: ParachainInfoRepository,
    assetLocations: Map<String, ReserveLocation>,
    chains: Map<ChainId, List<AssetTransfers>>
): TokenReserveRegistry {
    return TokenReserveRegistry(
        parachainInfoRepository = parachainInfoRepository,
        reservesById = assetLocations.mapValues { (_, reserve) ->
            TokenReserveConfig(
                reserveChainId = reserve.chainId,
                // Legacy config uses relative location for reserve, however in fact they are absolute
                // I decided to not to refactor it but rather simply perform conversion here in-place
                tokenReserveLocation = AbsoluteMultiLocation(reserve.multiLocation.interior)
            )
        },
        assetToReserveIdOverrides = buildMap {
            chains.forEach { (chainId, chainAssets) ->
                chainAssets.map { chainAssetConfig ->
                    val key = FullChainAssetId(chainId, chainAssetConfig.assetId)
                    // We could check that the `assetLocation` differs from the asset symbol to avoid placing redundant overrides...
                    // But we don't since it does not matter much anyway
                    put(key, chainAssetConfig.assetLocation)
                }
            }
        }
    )
}

private fun mapNetworkDeliveryFeeFromRemote(networkDeliveryFeeRemote: LegacyNetworkDeliveryFeeRemote): DeliveryFeeConfiguration {
    return DeliveryFeeConfiguration(
        toParent = mapDeliveryFeeConfigFromRemote(networkDeliveryFeeRemote.toParent),
        toParachain = mapDeliveryFeeConfigFromRemote(networkDeliveryFeeRemote.toParachain)
    )
}

private fun mapDeliveryFeeConfigFromRemote(config: LegacyDeliveryFeeConfigRemote?): DeliveryFeeConfiguration.Type? {
    if (config == null) return null

    return when (config.type) {
        "exponential" -> DeliveryFeeConfiguration.Type.Exponential(
            factorPallet = config.factorPallet,
            sizeBase = config.sizeBase,
            sizeFactor = config.sizeFactor,
            alwaysHoldingPays = config.alwaysHoldingPays ?: false
        )

        else -> throw IllegalArgumentException("Unknown delivery fee config type: ${config.type}")
    }
}

private fun mapReserveLocationFromRemote(reserveLocationRemote: LegacyReserveLocationRemote): ReserveLocation {
    return ReserveLocation(
        chainId = reserveLocationRemote.chainId,
        reserveFee = reserveLocationRemote.reserveFee?.let(::mapXcmFeeFromRemote),
        multiLocation = mapJunctionsRemoteToMultiLocation(reserveLocationRemote.multiLocation)
    )
}

private fun mapAssetTransfersFromRemote(remote: LegacyCrossChainOriginAssetRemote): AssetTransfers {
    val assetLocationPath = when (remote.assetLocationPath.type) {
        "absolute" -> AssetLocationPath.Absolute
        "relative" -> AssetLocationPath.Relative
        "concrete" -> {
            val junctionsRemote = remote.assetLocationPath.path!!

            AssetLocationPath.Concrete(mapJunctionsRemoteToMultiLocation(junctionsRemote))
        }

        else -> throw IllegalArgumentException("Unknown asset type")
    }

    return AssetTransfers(
        assetId = remote.assetId,
        assetLocationPath = assetLocationPath,
        assetLocation = remote.assetLocation,
        xcmTransfers = remote.xcmTransfers.map(::mapXcmTransferFromRemote)
    )
}

private fun mapXcmTransferFromRemote(remote: LegacyXcmTransferRemote): XcmTransfer {
    return XcmTransfer(
        destination = mapXcmDestinationFromRemote(remote.destination),
        type = mapXcmTransferTypeFromRemote(remote.type)
    )
}

private fun mapXcmTransferTypeFromRemote(remote: String): LegacyXcmTransferMethod {
    return when (remote) {
        "xtokens" -> LegacyXcmTransferMethod.X_TOKENS
        "xcmpallet" -> LegacyXcmTransferMethod.XCM_PALLET_RESERVE
        "xcmpallet-teleport" -> LegacyXcmTransferMethod.XCM_PALLET_TELEPORT
        "xcmpallet-transferAssets" -> LegacyXcmTransferMethod.XCM_PALLET_TRANSFER_ASSETS
        else -> LegacyXcmTransferMethod.UNKNOWN
    }
}

private fun mapXcmDestinationFromRemote(remote: LegacyXcmDestinationRemote): XcmDestination {
    return XcmDestination(
        chainId = remote.chainId,
        assetId = remote.assetId,
        fee = mapXcmFeeFromRemote(remote.fee)
    )
}

private fun mapXcmFeeFromRemote(
    remote: LegacyXcmFeeRemote
): XcmFee<String> {
    val mode = when (remote.mode.type) {
        "proportional" -> XcmFee.Mode.Proportional(remote.mode.value.asGsonParsedNumber())
        "standard" -> XcmFee.Mode.Standard
        else -> XcmFee.Mode.Unknown
    }

    return XcmFee(
        mode = mode,
        instructions = remote.instructions
    )
}

private fun mapXcmInstructionFromRemote(instruction: String): XCMInstructionType = runCatching {
    enumValueOf<XCMInstructionType>(instruction)
}.getOrDefault(XCMInstructionType.UNKNOWN)
