package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferFeatures
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.destinationChainLocationOnOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferReserve
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.buyExecution
import io.novafoundation.nova.feature_xcm_api.builder.createWithoutFeesMeasurement
import io.novafoundation.nova.feature_xcm_api.builder.withdrawAsset
import io.novafoundation.nova.feature_xcm_api.extrinsic.composeXcmExecute
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.getInnerSuccessOrThrow
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
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
            composeTransferAssetsCall(configuration, transfer, crossChainFee)
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

    private suspend fun composeTransferAssetsCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): GenericCall.Instance {
        val totalTransferAmount = transfer.amountPlanks + crossChainFee
        val multiAsset = MultiAsset.from(configuration.assetLocationOnOrigin, totalTransferAmount)

        return chainRegistry.withRuntime(configuration.originChainId) {
            composeCall(
                moduleName = metadata.xcmPalletName(),
                callName = "transfer_assets",
                arguments = mapOf(
                    "dest" to configuration.destinationChainLocationOnOrigin().versionedXcm().toEncodableInstance(),
                    "beneficiary" to transfer.beneficiaryLocation().versionedXcm().toEncodableInstance(),
                    "assets" to MultiAssets(multiAsset).versionedXcm().toEncodableInstance(),
                    "fee_asset_item" to BigInteger.ZERO,
                    "weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
                )
            )
        }
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
            xcmVersion = XcmVersion.V4
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

        when (val transferType = configuration.transferType) {
            XcmTransferReserve.Teleport -> buildTeleportProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )

            XcmTransferReserve.Reserve.Origin -> buildOriginReserveProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )

            XcmTransferReserve.Reserve.Destination -> buildDestinationReserveProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                amount = totalTransferAmount
            )

            is XcmTransferReserve.Reserve.Remote -> buildRemoteReserveProgram(
                assetLocation = configuration.assetAbsoluteLocation,
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
        val feesAmount = amount / 2.toBigInteger()

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
        val feesAmount = amount / 2.toBigInteger()

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
        val feesAmount = amount / 2.toBigInteger()

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
        val feesAmount = amount / 2.toBigInteger()

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

    private fun <T> T.versionedXcm() = versionedXcm(USED_XCM_VERSION)

    private fun AssetTransferBase.beneficiaryLocation(): RelativeMultiLocation {
        val accountId = destinationChain.accountIdOrDefault(recipient).intoKey()
        return accountId.toMultiLocation()
    }

    private fun WeightLimit.Companion.one(): WeightLimit.Limited {
        return WeightLimit.Limited(WeightV2(1.toBigInteger(), 1.toBigInteger()))
    }
}
