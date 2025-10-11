package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.feature_xcm_api.config.remote.JunctionsRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DynamicCrossChainTransfersConfigRemote(
    val chains: List<DynamicCrossChainOriginChainRemote>?,
    val customTeleports: List<CustomTeleportEntryRemote>?,
)

class CustomTeleportEntryRemote(
    val originChain: String,
    val destChain: String,
    val originAsset: Int
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
)
