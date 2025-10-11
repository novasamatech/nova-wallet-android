package io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain

import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration.CustomTeleportEntry
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration.TransferDestination
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveConfig
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.CustomTeleportEntryRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainOriginChainRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicReserveLocationRemote
import io.novafoundation.nova.feature_xcm_api.config.remote.toAbsoluteLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

fun DynamicCrossChainTransfersConfigRemote.toDomain(reserveRegistry: TokenReserveRegistry): DynamicCrossChainTransfersConfiguration {
    return DynamicCrossChainTransfersConfiguration(
        reserveRegistry = reserveRegistry,
        chains = constructChains(chains),
        customTeleports = constructCustomTeleports(customTeleports)
    )
}

private fun constructCustomTeleports(
    customTeleports: List<CustomTeleportEntryRemote>?
): Set<CustomTeleportEntry> {
    return customTeleports.orEmpty().mapToSet { entry ->
        with(entry) {
            CustomTeleportEntry(FullChainAssetId(originChain, originAsset), destChain)
        }
    }
}

private fun constructChains(
    chains: List<DynamicCrossChainOriginChainRemote>?
): Map<ChainId, List<AssetTransfers>> {
    return chains.orEmpty().associateBy(
        keySelector = DynamicCrossChainOriginChainRemote::chainId,
        valueTransform = ::constructTransfersForChain
    )
}

private fun constructTransfersForChain(configRemote: DynamicCrossChainOriginChainRemote): List<AssetTransfers> {
    return configRemote.assets.map { assetConfig ->
        AssetTransfers(
            assetId = assetConfig.assetId,
            destinations = assetConfig.xcmTransfers.map { transfer ->
                TransferDestination(
                    fullChainAssetId = FullChainAssetId(transfer.chainId, transfer.assetId),
                    hasDeliveryFee = transfer.hasDeliveryFee ?: false,
                    supportsXcmExecute = transfer.supportsXcmExecute ?: false,
                )
            }
        )
    }
}

private fun DynamicReserveLocationRemote.toDomain(): TokenReserveConfig {
    return TokenReserveConfig(
        reserveChainId = chainId,
        tokenReserveLocation = multiLocation.toAbsoluteLocation()
    )
}
