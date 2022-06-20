package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CrossChainTransfersConfigRemote(
    val assetsLocation: Map<String, ReserveLocationRemote>,
    val instructions: Map<String, List<String>>,
    val chains: List<CrossChainOriginChainRemote>
)

class ReserveLocationRemote(
    val chainId: ChainId,
    val multiLocation: JunctionsRemote
)

class CrossChainOriginChainRemote(
    val chainId: ChainId,
    val assets: List<CrossChainOriginAssetRemote>
)

class CrossChainOriginAssetRemote(
    val assetId: Int,
    val assetLocation: String,
    val assetLocationPath: AssetLocationPathRemote,
    val xcmTransfers: List<XcmTransferRemote>,
    val reserveFee: XcmFeeRemote?
)

class XcmTransferRemote(
    val destination: XcmDestinationRemote,
    val type: String,
)

class XcmDestinationRemote(
    val chainId: ChainId,
    val assetId: Int,
    val fee: XcmFeeRemote
)

class XcmFeeRemote(
    val mode: Mode,
    val instructions: String
) {

    class Mode(
        val type: String,
        val value: String?
    )
}

class AssetLocationPathRemote(
    val type: String,
    val path: JunctionsRemote?
)

typealias JunctionsRemote = Map<String, Any?>
