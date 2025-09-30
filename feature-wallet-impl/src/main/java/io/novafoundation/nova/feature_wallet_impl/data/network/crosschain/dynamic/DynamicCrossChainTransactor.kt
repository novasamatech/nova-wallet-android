package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.destinationChainLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferFeatures
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.originChainId
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.originChainLocation
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.common.TransferAssetUsingTypeTransactor
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.buyExecution
import io.novafoundation.nova.feature_xcm_api.builder.createWithoutFeesMeasurement
import io.novafoundation.nova.feature_xcm_api.builder.withdrawAsset
import io.novafoundation.nova.feature_xcm_api.extrinsic.composeXcmExecute
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.getInnerSuccessOrThrow
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import java.math.BigInteger
import javax.inject.Inject

private val USED_XCM_VERSION = XcmVersion.V4

@FeatureScope
class DynamicCrossChainTransactor @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val xcmBuilderFactory: XcmBuilder.Factory,
    private val xcmPaymentApi: XcmPaymentApi,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val usingTypeTransactor: TransferAssetUsingTypeTransactor,
) {

    context(ExtrinsicBuilder)
    suspend fun crossChainTransfer(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        val call = composeCrossChainTransferCall(configuration, transfer, crossChainFee)
        call(call)
    }

    suspend fun requiredRemainingAmountAfterTransfer(
        configuration: DynamicCrossChainTransferConfiguration
    ): Balance {
        return if (supportsXcmExecute(configuration)) {
            BigInteger.ZERO
        } else {
            val chainAsset = configuration.originChainAsset
            assetSourceRegistry.sourceFor(chainAsset).balance.existentialDeposit(chainAsset)
        }
    }

    suspend fun composeCrossChainTransferCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): GenericCall.Instance {
        return if (supportsXcmExecute(configuration)) {
            composeXcmExecuteCall(configuration, transfer, crossChainFee)
        } else {
            usingTypeTransactor.composeCall(configuration, transfer, crossChainFee, forceXcmVersion = USED_XCM_VERSION)
        }
    }

    suspend fun supportsXcmExecute(originChainId: ChainId, features: DynamicCrossChainTransferFeatures): Boolean {
        val supportsXcmExecute = features.supportsXcmExecute
        val hasXcmPaymentApi = xcmPaymentApi.isSupported(originChainId)

        // For now, only enable xcm execute approach for the directions that will hugely benefit from it
        // In particular, xcm execute allows us to pay delivery fee from the holding register and not in JIT mode (from account)
        val hasDeliveryFee = features.hasDeliveryFee

        return supportsXcmExecute && hasXcmPaymentApi && hasDeliveryFee
    }

    private suspend fun supportsXcmExecute(configuration: DynamicCrossChainTransferConfiguration): Boolean {
        return supportsXcmExecute(configuration.originChainId, configuration.features)
    }

    private suspend fun composeXcmExecuteCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): GenericCall.Instance {
        val xcmProgram = buildXcmProgram(configuration, transfer, crossChainFee)
        val weight = xcmPaymentApi.queryXcmWeight(configuration.originChainId, xcmProgram)
            .getInnerSuccessOrThrow("DynamicCrossChainTransactor")

        return chainRegistry.withRuntime(configuration.originChainId) {
            composeXcmExecute(xcmProgram, weight)
        }
    }

    private suspend fun buildXcmProgram(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): VersionedXcmMessage {
        val builder = xcmBuilderFactory.createWithoutFeesMeasurement(
            initial = configuration.originChainLocation,
            xcmVersion = USED_XCM_VERSION
        )

        builder.buildTransferProgram(configuration, transfer, crossChainFee)

        return builder.build()
    }

    private fun XcmBuilder.buildTransferProgram(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        val totalTransferAmount = transfer.amountPlanks + crossChainFee
        val assetAbsoluteMultiLocation = configuration.transferType.assetAbsoluteLocation

        when (val transferType = configuration.transferType) {
            is XcmTransferType.Teleport -> buildTeleportProgram(
                assetLocation = assetAbsoluteMultiLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )

            is XcmTransferType.Reserve.Origin -> buildOriginReserveProgram(
                assetLocation = assetAbsoluteMultiLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )

            is XcmTransferType.Reserve.Destination -> buildDestinationReserveProgram(
                assetLocation = assetAbsoluteMultiLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )

            is XcmTransferType.Reserve.Remote -> buildRemoteReserveProgram(
                assetLocation = assetAbsoluteMultiLocation,
                remoteReserveLocation = transferType.remoteReserveLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )
        }
    }

    private fun XcmBuilder.buildTeleportProgram(
        assetLocation: AbsoluteMultiLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        amount: Balance,
    ) {
        val feesAmount = deriveBuyExecutionUpperBoundAmount(amount)

        // Origin
        withdrawAsset(assetLocation, amount)
        // Here and onward: we use buy execution for the very first segment to be able to pay delivery fees in sending asset
        // WeightLimit.one() is used since it doesn't matter anyways as the message on origin is already weighted
        // The only restriction is that it cannot be zero or Unlimited
        buyExecution(assetLocation, feesAmount, WeightLimit.one())
        initiateTeleport(MultiAssetFilter.singleCounted(), destinationChainLocation)

        // Destination
        buyExecution(assetLocation, feesAmount, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)
    }

    private fun XcmBuilder.buildOriginReserveProgram(
        assetLocation: AbsoluteMultiLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        amount: Balance,
    ) {
        val feesAmount = deriveBuyExecutionUpperBoundAmount(amount)

        // Origin
        withdrawAsset(assetLocation, amount)
        buyExecution(assetLocation, feesAmount, WeightLimit.one())
        depositReserveAsset(MultiAssetFilter.singleCounted(), destinationChainLocation)

        // Destination
        buyExecution(assetLocation, feesAmount, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)
    }

    private fun XcmBuilder.buildDestinationReserveProgram(
        assetLocation: AbsoluteMultiLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        amount: Balance,
    ) {
        val feesAmount = deriveBuyExecutionUpperBoundAmount(amount)

        // Origin
        withdrawAsset(assetLocation, amount)
        buyExecution(assetLocation, feesAmount, WeightLimit.one())
        initiateReserveWithdraw(MultiAssetFilter.singleCounted(), destinationChainLocation)

        // Destination
        buyExecution(assetLocation, feesAmount, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)
    }

    private fun XcmBuilder.buildRemoteReserveProgram(
        assetLocation: AbsoluteMultiLocation,
        remoteReserveLocation: ChainLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        amount: Balance,
    ) {
        val feesAmount = deriveBuyExecutionUpperBoundAmount(amount)

        // Origin
        withdrawAsset(assetLocation, amount)
        buyExecution(assetLocation, feesAmount, WeightLimit.one())
        initiateReserveWithdraw(MultiAssetFilter.singleCounted(), remoteReserveLocation)

        // Remote reserve
        buyExecution(assetLocation, feesAmount, WeightLimit.Unlimited)
        depositReserveAsset(MultiAssetFilter.singleCounted(), destinationChainLocation)

        // Destination
        buyExecution(assetLocation, feesAmount, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)
    }

    private fun deriveBuyExecutionUpperBoundAmount(transferringAmount: Balance): Balance {
        return transferringAmount / 2.toBigInteger()
    }

    private fun WeightLimit.Companion.one(): WeightLimit.Limited {
        return WeightLimit.Limited(WeightV2(1.toBigInteger(), 1.toBigInteger()))
    }
}
