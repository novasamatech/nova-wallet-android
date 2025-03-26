package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy

import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.JunctionsRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

class LegacyCrossChainTransfersConfigRemote(
    val assetsLocation: Map<String, LegacyReserveLocationRemote>?,
    val instructions: Map<String, List<String>>?,
    val networkDeliveryFee: Map<String, LegacyNetworkDeliveryFeeRemote>?,
    val networkBaseWeight: Map<String, BigInteger>?,
    val chains: List<LegacyCrossChainOriginChainRemote>?
)

class LegacyReserveLocationRemote(
    val chainId: ChainId,
    val reserveFee: LegacyXcmFeeRemote?,
    val multiLocation: JunctionsRemote
)

class LegacyNetworkDeliveryFeeRemote(
    val toParent: LegacyDeliveryFeeConfigRemote?,
    val toParachain: LegacyDeliveryFeeConfigRemote?
)

class LegacyDeliveryFeeConfigRemote(
    val type: String,
    val factorPallet: String,
    val sizeBase: BigInteger,
    val sizeFactor: BigInteger,
    val alwaysHoldingPays: Boolean?
)

class LegacyCrossChainOriginChainRemote(
    val chainId: ChainId,
    val assets: List<LegacyCrossChainOriginAssetRemote>
)

class LegacyCrossChainOriginAssetRemote(
    val assetId: Int,
    val assetLocation: String,
    val assetLocationPath: LegacyAssetLocationPathRemote,
    val xcmTransfers: List<LegacyXcmTransferRemote>,
)

class LegacyXcmTransferRemote(
    val destination: LegacyXcmDestinationRemote,
    val type: String,
)

class LegacyXcmDestinationRemote(
    val chainId: ChainId,
    val assetId: Int,
    val fee: LegacyXcmFeeRemote
)

class LegacyXcmFeeRemote(
    val mode: Mode,
    val instructions: String
) {

    class Mode(
        val type: String,
        val value: String?
    )
}

class LegacyAssetLocationPathRemote(
    val type: String,
    val path: JunctionsRemote?
)
