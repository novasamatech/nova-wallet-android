package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.di.scope.FeatureScope
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
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.orZero
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.plus
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.zero
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.destinationChainLocationOnOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.CrossChainFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.DeliveryFeeConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmFee.Mode
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.XCMInstructionType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.weightToFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.originChainId
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.xcmExecute
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction
import io.novafoundation.nova.feature_xcm_api.message.XcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.isHere
import io.novafoundation.nova.feature_xcm_api.multiLocation.paraIdOrNull
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.emptyAccountIdKey
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
import javax.inject.Inject
import javax.inject.Named

// TODO: Currently message doesn't contain setTopic command in the end. It will come with XCMv3 support
private const val SET_TOPIC_SIZE = 33

@FeatureScope
class LegacyCrossChainWeigher @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE)
    private val storageDataSource: StorageDataSource,
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val xcmVersionDetector: XcmVersionDetector
) {

    fun estimateRequiredDestWeight(transferConfiguration: LegacyCrossChainTransferConfiguration): Weight {
        val destinationWeight = transferConfiguration.destinationFee.estimatedWeight()
        val reserveWeight = transferConfiguration.reserveFee?.estimatedWeight().orZero()

        return destinationWeight.max(reserveWeight)
    }

    suspend fun estimateFee(amount: Balance, config: LegacyCrossChainTransferConfiguration): CrossChainFeeModel = with(config) {
        // Reserve fee may be zero if xcm transfer doesn't reserve tokens
        val reserveFeeAmount = calculateFee(amount, reserveFee, reserveChainLocation)
        val destinationFeeAmount = calculateFee(amount, destinationFee, destinationChainLocationOnOrigin())

        return reserveFeeAmount + destinationFeeAmount
    }

    private suspend fun LegacyCrossChainTransferConfiguration.calculateFee(
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

    private suspend fun LegacyCrossChainTransferConfiguration.feeFor(amount: Balance, feeConfig: CrossChainFeeConfiguration): CrossChainFeeModel {
        val chain = chainRegistry.getChain(feeConfig.to.chainId)
        val maxWeight = feeConfig.estimatedWeight()

        return when (val mode = feeConfig.to.xcmFeeType.mode) {
            is Mode.Proportional -> CrossChainFeeModel(paidFromHolding = mode.weightToFee(maxWeight))

            Mode.Standard -> {
                val xcmMessage = xcmMessage(feeConfig.to.xcmFeeType.instructions, chain, amount)

                val paymentInfo = extrinsicService.paymentInfo(
                    chain,
                    TransactionOrigin.SelectedWallet
                ) {
                    xcmExecute(xcmMessage, maxWeight)
                }

                CrossChainFeeModel(paidFromHolding = paymentInfo.partialFee)
            }

            Mode.Unknown -> CrossChainFeeModel.zero()
        }
    }

    private suspend fun LegacyCrossChainTransferConfiguration.deliveryFeeFor(
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
        val deliveryFee = BigRational.fixedU128(deliveryFeeFactor * feeSize).integralQuotient

        val isSenderPaysOriginDelivery = !deliveryConfig.alwaysHoldingPays
        return if (isSenderPaysOriginDelivery && isSendingFromOrigin) {
            CrossChainFeeModel(paidByAccount = deliveryFee)
        } else {
            CrossChainFeeModel(paidFromHolding = deliveryFee)
        }
    }

    private suspend fun LegacyCrossChainTransferConfiguration.getXcmMessageSize(amount: Balance, config: CrossChainFeeConfiguration): BigInteger {
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

    private suspend fun LegacyCrossChainTransferConfiguration.xcmMessage(
        instructionTypes: List<XCMInstructionType>,
        chain: Chain,
        amount: Balance
    ): VersionedXcm<XcmMessage> {
        val instructions = instructionTypes.mapNotNull { instructionType -> xcmInstruction(instructionType, chain, amount) }
        val message = XcmMessage(instructions)
        val xcmVersion = xcmVersionDetector.lowestPresentMultiLocationVersion(chain.id).orDefault()

        return message.versionedXcm(xcmVersion)
    }

    private fun LegacyCrossChainTransferConfiguration.xcmInstruction(
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

    private fun LegacyCrossChainTransferConfiguration.reserveAssetDeposited(amount: Balance) =
        XcmInstruction.ReserveAssetDeposited(
            assets = MultiAssets(
                sendingAssetAmountOf(amount)
            )
        )

    private fun LegacyCrossChainTransferConfiguration.receiveTeleportedAsset(amount: Balance) =
        XcmInstruction.ReceiveTeleportedAsset(
            assets = MultiAssets(
                sendingAssetAmountOf(amount)
            )
        )

    @Suppress("unused")
    private fun LegacyCrossChainTransferConfiguration.clearOrigin() = XcmInstruction.ClearOrigin

    private fun LegacyCrossChainTransferConfiguration.buyExecution(amount: Balance): XcmInstruction.BuyExecution {
        return XcmInstruction.BuyExecution(
            fees = sendingAssetAmountOf(amount),
            weightLimit = WeightLimit.Unlimited
        )
    }

    @Suppress("unused")
    private fun LegacyCrossChainTransferConfiguration.depositAsset(chain: Chain): XcmInstruction.DepositAsset {
        return XcmInstruction.DepositAsset(
            assets = MultiAssetFilter.Wild.All,
            beneficiary = chain.emptyBeneficiaryMultiLocation()
        )
    }

    private fun LegacyCrossChainTransferConfiguration.withdrawAsset(amount: Balance): XcmInstruction.WithdrawAsset {
        return XcmInstruction.WithdrawAsset(
            assets = MultiAssets(
                sendingAssetAmountOf(amount)
            )
        )
    }

    private fun LegacyCrossChainTransferConfiguration.depositReserveAsset(): XcmInstruction {
        return XcmInstruction.DepositReserveAsset(
            assets = MultiAssetFilter.Wild.All,
            dest = destinationChainLocationOnOrigin(),
            xcm = XcmMessage(emptyList())
        )
    }

    private fun LegacyCrossChainTransferConfiguration.sendingAssetAmountOf(planks: Balance): MultiAsset {
        return MultiAsset.from(
            amount = planks,
            multiLocation = assetLocation,
        )
    }

    private fun Chain.emptyBeneficiaryMultiLocation(): RelativeMultiLocation = emptyAccountIdKey().toMultiLocation()

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
