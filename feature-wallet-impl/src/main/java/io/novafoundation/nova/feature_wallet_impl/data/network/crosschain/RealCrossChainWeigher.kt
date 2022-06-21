package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFee
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee.Mode
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import io.novafoundation.nova.feature_wallet_api.domain.model.XCMInstructionType
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Fungibility
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Id
import io.novafoundation.nova.feature_wallet_impl.domain.crosschain.weightToFee
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class RealCrossChainWeigher(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry
) : CrossChainWeigher {

    override suspend fun estimateFee(transferConfiguration: CrossChainTransferConfiguration): CrossChainFee {
        val destinationFee = with(transferConfiguration) {
            feeFor(destinationFee)
        }

        return CrossChainFee(
            origin = BigInteger.ZERO, // TODO construct origin extrinsic
            destination = destinationFee,
            reserve = null // TODO reserve fee
        )
    }

    private suspend fun CrossChainTransferConfiguration.feeFor(feeConfig: CrossChainFeeConfiguration): BigInteger? {
        val chain = chainRegistry.getChain(feeConfig.chainId)
        val instructionTypes = feeConfig.xcmFeeType.instructions
        val maxWeight = feeConfig.instructionWeight * instructionTypes.size.toBigInteger()

        return when(val mode = feeConfig.xcmFeeType.mode) {
            is Mode.Proportional -> mode.weightToFee(maxWeight)

            Mode.Standard -> {
                val xcmMessage = xcmMessage(instructionTypes, chain)

                val paymentInfo = extrinsicService.paymentInfo(chain) {
                    xcmExecute(xcmMessage, maxWeight = maxWeight)
                }

                paymentInfo.partialFee
            }

            Mode.Unknown -> null
        }
    }

    private fun CrossChainTransferConfiguration.xcmMessage(
        instructionTypes: List<XCMInstructionType>,
        chain: Chain,
    ): XcmV2 {
        val instructions = instructionTypes.map { instructionType -> xcmInstruction(instructionType, chain) }

        return XcmV2(instructions)
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
