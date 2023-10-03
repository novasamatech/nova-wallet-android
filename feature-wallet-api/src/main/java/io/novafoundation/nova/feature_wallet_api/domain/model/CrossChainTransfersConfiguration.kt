package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

class CrossChainTransfersConfiguration(
    // Reserves locations from the Relaychain point of view
    val assetLocations: Map<String, ReserveLocation>,
    val feeInstructions: Map<String, List<XCMInstructionType>>,
    val instructionBaseWeights: Map<String, Weight>,
    val chains: Map<ChainId, List<AssetTransfers>>
) {

    class ReserveLocation(
        val chainId: ChainId,
        val reserveFee: XcmFee<String>?,
        val multiLocation: MultiLocation
    )

    class AssetTransfers(
        val assetId: ChainAssetId,
        val assetLocation: String,
        val assetLocationPath: AssetLocationPath,
        val xcmTransfers: List<XcmTransfer>
    )

    class XcmTransfer(
        val destination: XcmDestination,
        val type: XcmTransferType
    )

    class XcmDestination(
        val chainId: ChainId,
        val assetId: ChainAssetId,
        val fee: XcmFee<String>,
    )

    class XcmFee<I>(
        val mode: Mode,
        val instructions: I
    ) {
        sealed class Mode {
            object Standard : Mode()

            class Proportional(val unitsPerSecond: BigInteger) : Mode()

            object Unknown : Mode()
        }
    }
}

sealed class AssetLocationPath {

    object Relative : AssetLocationPath()

    object Absolute : AssetLocationPath()

    class Concrete(val multiLocation: MultiLocation) : AssetLocationPath()
}

enum class XcmTransferType {
    X_TOKENS, XCM_PALLET_RESERVE, XCM_PALLET_TELEPORT, UNKNOWN
}

enum class XCMInstructionType {
    ReserveAssetDeposited, ClearOrigin, BuyExecution, DepositAsset, WithdrawAsset, DepositReserveAsset, ReceiveTeleportedAsset, UNKNOWN
}

class CrossChainTransferConfiguration(
    val assetLocation: MultiLocation,
    val destinationChainLocation: MultiLocation,
    val destinationFee: CrossChainFeeConfiguration,
    val reserveFee: CrossChainFeeConfiguration?,
    val transferType: XcmTransferType
)

class CrossChainFeeConfiguration(
    val chainId: ChainId,
    val instructionWeight: Weight,
    val xcmFeeType: XcmFee<List<XCMInstructionType>>
)
