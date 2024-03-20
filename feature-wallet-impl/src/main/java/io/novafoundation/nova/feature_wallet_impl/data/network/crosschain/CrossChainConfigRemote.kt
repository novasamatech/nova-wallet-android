package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

class CrossChainTransfersConfigRemote(
    val assetsLocation: Map<String, ReserveLocationRemote>?,
    val instructions: Map<String, List<String>>?,
    val networkDeliveryFee: Map<String, NetworkDeliveryFeeRemote>?,
    val networkBaseWeight: Map<String, BigInteger>?,
    val chains: List<CrossChainOriginChainRemote>?
)

class ReserveLocationRemote(
    val chainId: ChainId,
    val reserveFee: XcmFeeRemote?,
    val multiLocation: JunctionsRemote
)

class NetworkDeliveryFeeRemote(
    val toParent: DeliveryFeeConfigRemote?,
    val toParachain: DeliveryFeeConfigRemote?
)

class DeliveryFeeConfigRemote(
    val type: String,
    val factorPallet: String,
    val sizeBase: BigInteger,
    val sizeFactor: BigInteger,
    val alwaysHoldingPays: Boolean?
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
