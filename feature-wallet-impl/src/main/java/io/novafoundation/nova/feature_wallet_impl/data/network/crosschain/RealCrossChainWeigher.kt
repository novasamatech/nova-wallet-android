package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.BigRational
import io.novafoundation.nova.common.utils.argument
import io.novafoundation.nova.common.utils.fixedU128
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.requireActualType
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.orZero
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.plus
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.zero
import io.novafoundation.nova.feature_wallet_api.domain.implementations.accountIdToMultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.implementations.weightToFee
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration.XcmFee.Mode
import io.novafoundation.nova.feature_wallet_api.domain.model.DeliveryFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.XCMInstructionType
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset.Fungibility
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction
import io.novafoundation.nova.feature_xcm_api.message.XcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.isHere
import io.novafoundation.nova.feature_xcm_api.multiLocation.paraIdOrNull
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.definitions.types.bytes
import io.novasama.substrate_sdk_android.runtime.metadata.call
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

// TODO: Currently message doesn't contain setTopic command in the end. It will come with XCMv3 support
private const val SET_TOPIC_SIZE = 33

class RealCrossChainWeigher(
    private val storageDataSource: StorageDataSource,
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val xcmVersionDetector: XcmVersionDetector
) : CrossChainWeigher {

    override suspend fun estimateRequiredDestWeight(transferConfiguration: CrossChainTransferConfiguration): Weight {
        val destinationWeight = transferConfiguration.destinationFee.estimatedWeight()
        val reserveWeight = transferConfiguration.reserveFee?.estimatedWeight().orZero()

        return destinationWeight.max(reserveWeight)
    }

    override suspend fun estimateFee(amount: Balance, config: CrossChainTransferConfiguration): CrossChainFeeModel = with(config) {
        // Reserve fee may be zero if xcm transfer doesn't reserve tokens
        val reserveFeeAmount = calculateFee(amount, reserveFee, reserveChainLocation)
        val destinationFeeAmount = calculateFee(amount, destinationFee, destinationChainLocation)

        return reserveFeeAmount + destinationFeeAmount
    }

    private suspend fun CrossChainTransferConfiguration.calculateFee(
        amount: Balance,
        feeConfig: CrossChainFeeConfiguration?,
        chainLocation: RelativeMultiLocation
    ): CrossChainFeeModel {
        return when (feeConfig) {
            null -> CrossChainFeeModel.zero()
            else -> {
                val isSendingFromOrigin = originChainId == feeConfig.from.chainId
                val feeAmount = feeFor(amount, feeConfig)
                val deliveryFee = deliveryFeeFor(amount, feeConfig, chainLocation, isSendingFromOrigin = isSendingFromOrigin)
                feeAmount.orZero() + deliveryFee.orZero()
            }
        }
    }

    private suspend fun CrossChainTransferConfiguration.feeFor(amount: Balance, feeConfig: CrossChainFeeConfiguration): CrossChainFeeModel {
        val chain = chainRegistry.getChain(feeConfig.to.chainId)
        val maxWeight = feeConfig.estimatedWeight()

        return when (val mode = feeConfig.to.xcmFeeType.mode) {
            is Mode.Proportional -> CrossChainFeeModel(executionFees = mode.weightToFee(maxWeight))

            Mode.Standard -> {
                val xcmMessage = xcmMessage(feeConfig.to.xcmFeeType.instructions, chain, amount)

                val paymentInfo = extrinsicService.paymentInfo(chain, TransactionOrigin.SelectedWallet) {
                    xcmExecute(xcmMessage, maxWeight)
                }

                CrossChainFeeModel(executionFees = paymentInfo.partialFee)
            }

            Mode.Unknown -> CrossChainFeeModel.zero()
        }
    }

    private suspend fun CrossChainTransferConfiguration.deliveryFeeFor(
        amount: Balance,
        config: CrossChainFeeConfiguration,
        destinationChainLocation: RelativeMultiLocation,
        isSendingFromOrigin: Boolean
    ): CrossChainFeeModel {
        val deliveryFeeConfiguration = config.from.deliveryFeeConfiguration ?: return CrossChainFeeModel.zero()

        val deliveryConfig = deliveryFeeConfiguration.getDeliveryConfig(destinationChainLocation)

        val deliveryFeeFactor: BigInteger = queryDeliveryFeeFactor(config.from.chainId, deliveryConfig.factorPallet, destinationChainLocation)

        val xcmMessageSize = getXcmMessageSize(amount, config)
        val xcmMessageSizeWithTopic = xcmMessageSize + SET_TOPIC_SIZE.toBigInteger()

        val feeSize = (deliveryConfig.sizeBase + xcmMessageSizeWithTopic * deliveryConfig.sizeFactor)
        val deliveryFee = BigRational.fixedU128(deliveryFeeFactor * feeSize)

        val isSenderPaysOriginDelivery = !deliveryConfig.alwaysHoldingPays
        return if (isSenderPaysOriginDelivery && isSendingFromOrigin) {
            CrossChainFeeModel(deliveryFees = deliveryFee)
        } else {
            CrossChainFeeModel(executionFees = deliveryFee)
        }
    }

    private suspend fun CrossChainTransferConfiguration.getXcmMessageSize(amount: Balance, config: CrossChainFeeConfiguration): BigInteger {
        val chain = chainRegistry.getChain(config.to.chainId)
        val runtime = chainRegistry.getRuntime(config.to.chainId)
        val xcmMessage = xcmMessage(config.to.xcmFeeType.instructions, chain, amount)
            .toEncodableInstance()

        return runtime.metadata
            .module(runtime.metadata.xcmPalletName())
            .call("execute")
            .argument("message")
            .requireActualType()
            .bytes(runtime, xcmMessage)
            .size.toBigInteger()
    }

    private fun DeliveryFeeConfiguration.getDeliveryConfig(destinationChainLocation: RelativeMultiLocation): DeliveryFeeConfiguration.Type.Exponential {
        val isParent = destinationChainLocation.interior.isHere()

        val configType = when {
            isParent -> toParent
            else -> toParachain
        }

        return configType.asExponentialOrThrow()
    }

    private fun DeliveryFeeConfiguration.Type?.asExponentialOrThrow(): DeliveryFeeConfiguration.Type.Exponential {
        return this as? DeliveryFeeConfiguration.Type.Exponential ?: throw IllegalStateException("Unknown delivery fee type")
    }

    private suspend fun queryDeliveryFeeFactor(
        chainId: ChainId,
        pallet: String,
        destinationMultiLocation: RelativeMultiLocation,
    ): BigInteger {
        return when {
            destinationMultiLocation.interior.isHere() -> xcmParentDeliveryFeeFactor(chainId, pallet)
            else -> {
                val paraId = destinationMultiLocation.paraIdOrNull() ?: throw IllegalStateException("ParaId must be not null")
                xcmParachainDeliveryFeeFactor(chainId, pallet, paraId)
            }
        }
    }

    private fun CrossChainFeeConfiguration.estimatedWeight(): Weight {
        val instructionTypes = to.xcmFeeType.instructions

        return to.instructionWeight * instructionTypes.size.toBigInteger()
    }

    private suspend fun CrossChainTransferConfiguration.xcmMessage(
        instructionTypes: List<XCMInstructionType>,
        chain: Chain,
        amount: Balance
    ): VersionedXcm<XcmMessage> {
        val instructions = instructionTypes.mapNotNull { instructionType -> xcmInstruction(instructionType, chain, amount) }
        val message = XcmMessage(instructions)
        val xcmVersion = xcmVersionDetector.lowestPresentMultiLocationVersion(chain.id).orDefault()

        return message.versionedXcm(xcmVersion)
    }

    private fun CrossChainTransferConfiguration.xcmInstruction(
        instructionType: XCMInstructionType,
        chain: Chain,
        amount: Balance
    ): XcmInstruction? {
        return when (instructionType) {
            XCMInstructionType.ReserveAssetDeposited -> reserveAssetDeposited(amount)
            XCMInstructionType.ClearOrigin -> clearOrigin()
            XCMInstructionType.BuyExecution -> buyExecution(amount)
            XCMInstructionType.DepositAsset -> depositAsset(chain)
            XCMInstructionType.WithdrawAsset -> withdrawAsset(amount)
            XCMInstructionType.DepositReserveAsset -> depositReserveAsset()
            XCMInstructionType.ReceiveTeleportedAsset -> receiveTeleportedAsset(amount)
            XCMInstructionType.UNKNOWN -> null
        }
    }

    private fun CrossChainTransferConfiguration.reserveAssetDeposited(amount: Balance) = XcmInstruction.ReserveAssetDeposited(
        assets = MultiAssets(
            sendingAssetAmountOf(amount)
        )
    )

    private fun CrossChainTransferConfiguration.receiveTeleportedAsset(amount: Balance) = XcmInstruction.ReceiveTeleportedAsset(
        assets = MultiAssets(
            sendingAssetAmountOf(amount)
        )
    )

    @Suppress("unused")
    private fun CrossChainTransferConfiguration.clearOrigin() = XcmInstruction.ClearOrigin

    private fun CrossChainTransferConfiguration.buyExecution(amount: Balance): XcmInstruction.BuyExecution {
        return XcmInstruction.BuyExecution(
            fees = sendingAssetAmountOf(amount),
            weightLimit = WeightLimit.Unlimited
        )
    }

    @Suppress("unused")
    private fun CrossChainTransferConfiguration.depositAsset(chain: Chain): XcmInstruction.DepositAsset {
        return XcmInstruction.DepositAsset(
            assets = MultiAssetFilter.Wild.All,
            maxAssets = BigInteger.ONE,
            beneficiary = chain.emptyBeneficiaryMultiLocation()
        )
    }

    private fun CrossChainTransferConfiguration.withdrawAsset(amount: Balance): XcmInstruction.WithdrawAsset {
        return XcmInstruction.WithdrawAsset(
            assets = MultiAssets(
                sendingAssetAmountOf(amount)
            )
        )
    }

    private fun CrossChainTransferConfiguration.depositReserveAsset(): XcmInstruction {
        return XcmInstruction.DepositReserveAsset(
            assets = MultiAssetFilter.Wild.All,
            maxAssets = BigInteger.ONE,
            dest = destinationChainLocation,
            xcm = XcmMessage(emptyList())
        )
    }

    private fun CrossChainTransferConfiguration.sendingAssetAmountOf(planks: Balance): MultiAsset {
        return MultiAsset(
            fungibility = Fungibility.Fungible(amount = planks),
            id = MultiAssetId(assetLocation)
        )
    }

    private fun Chain.emptyBeneficiaryMultiLocation(): RelativeMultiLocation = emptyAccountId().accountIdToMultiLocation()

    private suspend fun xcmParachainDeliveryFeeFactor(chainId: ChainId, moduleName: String, paraId: ParaId): BigInteger {
        return storageDataSource.query(chainId, applyStorageDefault = true) {
            runtime.metadata.module(moduleName).storage("DeliveryFeeFactor")
                .query(
                    paraId,
                    binding = ::bindNumber
                )
        }
    }

    private suspend fun xcmParentDeliveryFeeFactor(chainId: ChainId, moduleName: String): BigInteger {
        return storageDataSource.query(chainId) {
            runtime.metadata.module(moduleName).storage("UpwardDeliveryFeeFactor")
                .query(binding = ::bindNumber)
        }
    }
}
