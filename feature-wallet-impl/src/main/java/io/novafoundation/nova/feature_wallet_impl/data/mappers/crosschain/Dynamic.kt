package io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain

import io.novafoundation.nova.common.utils.flattenKeys
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransfersConfiguration.TransferDestination
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainOriginChainRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicReserveLocationRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

fun DynamicCrossChainTransfersConfigRemote.toDomain(): DynamicCrossChainTransfersConfiguration {
    return DynamicCrossChainTransfersConfiguration(
        reserveRegistry = constructReserveRegistry(assetsLocation, reserveIdOverrides),
        chains = constructChains(chains)
    )
}

private fun constructReserveRegistry(
    assetsLocation: Map<String, DynamicReserveLocationRemote>?,
    reserveIdOverrides: Map<String, Map<Int, String>>?,
): TokenReserveRegistry {
    return TokenReserveRegistry(
        reservesById = assetsLocation.orEmpty().mapValues { (_, reserve) ->
            reserve.toDomain()
        },
        assetToReserveIdOverrides = reserveIdOverrides.orEmpty().flattenKeys(::FullChainAssetId)
    )
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
                    hasDeliveryFee = transfer.hasDeliveryFee,
                    supportsXcmExecute = transfer.supportsXcmExecute
                )
            }
        )
    }
}

private fun DynamicReserveLocationRemote.toDomain(): TokenReserve {
    return TokenReserve(
        chainId = chainId,
        location = multiLocation.toAbsoluteLocation()
    )
}
