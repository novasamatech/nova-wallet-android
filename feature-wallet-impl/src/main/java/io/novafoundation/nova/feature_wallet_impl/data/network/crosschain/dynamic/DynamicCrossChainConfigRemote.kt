package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.JunctionsRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DynamicCrossChainTransfersConfigRemote(
    val assetsLocation: Map<String, DynamicReserveLocationRemote>?,
    // (ChainId, AssetId) -> ReserveId
    val reserveIdOverrides: Map<String, Map<Int, String>>,
    val chains: List<DynamicCrossChainOriginChainRemote>?
)

class DynamicReserveLocationRemote(
    val chainId: ChainId,
    val multiLocation: JunctionsRemote
)

class DynamicCrossChainOriginChainRemote(
    val chainId: ChainId,
    val assets: List<DynamicCrossChainOriginAssetRemote>
)

class DynamicCrossChainOriginAssetRemote(
    val assetId: Int,
    val xcmTransfers: List<DynamicXcmTransferRemote>,
)

class DynamicXcmTransferRemote(
    val chainId: ChainId,
    val assetId: Int,
    val hasDeliveryFee: Boolean?,
    val supportsXcmExecute: Boolean?,
    val usesTeleport: Boolean?
)
