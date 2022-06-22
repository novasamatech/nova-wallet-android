package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFee
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.implementations.weightToFee
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee.Mode
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import io.novafoundation.nova.feature_wallet_api.domain.model.XCMInstructionType
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Fungibility
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Id
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

private val XCM_EXECUTE_WEIGHT_OVERHEAD = 100_000_000.toBigInteger()

class RealCrossChainWeigher(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry
) : CrossChainWeigher {

    override suspend fun estimateRequiredDestWeight(transferConfiguration: CrossChainTransferConfiguration): Weight {
        val destinationWeight = transferConfiguration.destinationFee.estimatedWeight()
        val reserveWeight = transferConfiguration.reserveFee?.estimatedWeight().orZero()

        return destinationWeight.max(reserveWeight)
    }

    override suspend fun estimateFee(transferConfiguration: CrossChainTransferConfiguration): CrossChainFee {
        val destinationFee = with(transferConfiguration) {
            feeFor(destinationFee)
        }

        val reserveFee = with(transferConfiguration) {
            reserveFee?.let { feeFor(it) }
        }

        return CrossChainFee(
            destination = destinationFee,
            reserve = reserveFee
        )
    }

    private suspend fun CrossChainTransferConfiguration.feeFor(feeConfig: CrossChainFeeConfiguration): BigInteger? {
        val chain = chainRegistry.getChain(feeConfig.chainId)
        val maxWeight = feeConfig.estimatedWeight()

        return when (val mode = feeConfig.xcmFeeType.mode) {
            is Mode.Proportional -> mode.weightToFee(maxWeight)

            Mode.Standard -> {
                val xcmMessage = xcmMessage(feeConfig.xcmFeeType.instructions, chain)

                // xcmPallet.execute() has weight equal to maxWeight + XCM_EXECUTE_WEIGHT_OVERHEAD.
                // For more accurate calculations we should subtract overhead value from the maxWeight to adjust resulting weight
                val maxWeightForXcmExecute = maxWeight - XCM_EXECUTE_WEIGHT_OVERHEAD

                val paymentInfo = extrinsicService.paymentInfo(chain) {
                    xcmExecute(xcmMessage, maxWeight = maxWeightForXcmExecute)
                }

                paymentInfo.partialFee
            }

            Mode.Unknown -> null
        }
    }

    private fun CrossChainFeeConfiguration.estimatedWeight(): Weight {
        val instructionTypes = xcmFeeType.instructions

        return instructionWeight * instructionTypes.size.toBigInteger()
    }

    private fun CrossChainTransferConfiguration.xcmMessage(
        instructionTypes: List<XCMInstructionType>,
        chain: Chain,
    ): VersionedXcm {
        val instructions = instructionTypes.map { instructionType -> xcmInstruction(instructionType, chain) }

        return VersionedXcm.V2(XcmV2(instructions))
    }

    private fun CrossChainTransferConfiguration.xcmInstruction(
        instructionType: XCMInstructionType,
        chain: Chain,
    ): XcmV2Instruction {
        return when (instructionType) {
            XCMInstructionType.ReserveAssetDeposited -> reserveAssetDeposited()
            XCMInstructionType.ClearOrigin -> clearOrigin()
            XCMInstructionType.BuyExecution -> buyExecution()
            XCMInstructionType.DepositAsset -> depositAsset(chain)
            XCMInstructionType.WithdrawAsset -> withdrawAsset()
            XCMInstructionType.DepositReserveAsset -> depositReserveAsset()
        }
    }

    private fun CrossChainTransferConfiguration.reserveAssetDeposited() = XcmV2Instruction.ReserveAssetDeposited(
        assets = listOf(
            sendingAssetAmountOf(BigInteger.ZERO)
        )
    )

    @Suppress("unused")
    private fun CrossChainTransferConfiguration.clearOrigin() = XcmV2Instruction.ClearOrigin

    private fun CrossChainTransferConfiguration.buyExecution(): XcmV2Instruction.BuyExecution {
        return XcmV2Instruction.BuyExecution(
            fees = sendingAssetAmountOf(Balance.ZERO),
            weightLimit = WeightLimit.Limited(Weight.ZERO)
        )
    }

    @Suppress("unused")
    private fun CrossChainTransferConfiguration.depositAsset(chain: Chain): XcmV2Instruction.DepositAsset {
        return XcmV2Instruction.DepositAsset(
            assets = XcmMultiAssetFilter.Wild.All,
            maxAssets = BigInteger.ONE,
            beneficiary = chain.emptyBeneficiaryMultiLocation()
        )
    }

    private fun CrossChainTransferConfiguration.withdrawAsset(): XcmV2Instruction.WithdrawAsset {
        return XcmV2Instruction.WithdrawAsset(
            assets = listOf(
                sendingAssetAmountOf(Balance.ZERO)
            )
        )
    }

    private fun CrossChainTransferConfiguration.depositReserveAsset(): XcmV2Instruction {
        return XcmV2Instruction.DepositReserveAsset(
            assets = XcmMultiAssetFilter.Wild.All,
            maxAssets = BigInteger.ONE,
            dest = destinationChainLocation,
            xcm = XcmV2(emptyList())
        )
    }

    private fun CrossChainTransferConfiguration.sendingAssetAmountOf(planks: Balance): XcmMultiAsset {
        return XcmMultiAsset(
            fungibility = Fungibility.Fungible(amount = planks),
            id = Id.Concrete(assetLocation)
        )
    }

    private fun Chain.emptyBeneficiaryMultiLocation(): MultiLocation = MultiLocation(
        parents = BigInteger.ZERO,
        interior = MultiLocation.Interior.Junctions(
            junctions = listOf(
                if (isEthereumBased) Junction.AccountKey20(ByteArray(20))
                else Junction.AccountId32(ByteArray(32))
            )
        )
    )
}
