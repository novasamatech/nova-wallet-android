package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee.PostSubmissionAmountFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.destinationChainLocationOnOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.transferType
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.AmountWithdrawMode
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.buyExecution
import io.novafoundation.nova.feature_xcm_api.builder.createWithoutFeesMeasurement
import io.novafoundation.nova.feature_xcm_api.builder.relativeToLocal
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
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger
import javax.inject.Inject

private val USED_XCM_VERSION = XcmVersion.V4

@FeatureScope
class DynamicCrossChainTransactor @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val xcmBuilderFactory: XcmBuilder.Factory,
    private val xcmPaymentApi: XcmPaymentApi
) {

    context(ExtrinsicBuilder)
    suspend fun crossChainTransfer(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        mode: AmountWithdrawMode,
        fee: PostSubmissionAmountFee?,
    ) {
        val call = composeCrossChainTransferCall(configuration, transfer, mode, fee).value
        call(call)
    }

    suspend fun composeCrossChainTransferCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        mode: AmountWithdrawMode,
        fee: PostSubmissionAmountFee?,
    ): DynamicXcmBuildResult<GenericCall.Instance> {
        val addForFees = fee?.totalFee?.amount.orZero()
        val totalTransferAmount = mode.totalTransferAmount(transfer.amountPlanks, addForFees)

        return if (configuration.hasDeliveryFee) {
            // Apply new xcm.execute logic only for cases when we have delivery fees as "transfer_assets" cannot handle them for non-native assets
            // We can fully switch to this in the future once we are sure all the relevant chains use XcmExecuteFilter=Everything
            composeXcmExecuteCall(configuration, transfer, totalTransferAmount, fee)
        } else {
            composeTransferAssetsCall(configuration, transfer, totalTransferAmount)
        }
    }

    private suspend fun composeTransferAssetsCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        totalToWithdraw: Balance,
    ): DynamicXcmBuildResult<GenericCall.Instance> {
        val multiAsset = MultiAsset.from(configuration.assetLocationOnOrigin, totalToWithdraw)

        return chainRegistry.withRuntime(configuration.originChainId) {
            val call = composeCall(
                moduleName = metadata.xcmPalletName(),
                callName = "transfer_assets",
                args = mapOf(
                    "dest" to configuration.destinationChainLocationOnOrigin().versionedXcm().toEncodableInstance(),
                    "beneficiary" to transfer.beneficiaryLocation().versionedXcm().toEncodableInstance(),
                    "assets" to MultiAssets(multiAsset).versionedXcm().toEncodableInstance(),
                    "fee_asset_item" to BigInteger.ZERO,
                    "weight_limit" to WeightLimit.Unlimited.toEncodableInstance()
                )
            )

            DynamicXcmBuildResult(call, specifiedFees = AlwaysPaidFromBuyExecution)
        }
    }

    private suspend fun composeXcmExecuteCall(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?
    ): DynamicXcmBuildResult<GenericCall.Instance> {
        return buildXcmProgram(configuration, transfer, totalToWithdraw, fee).map { xcmProgram ->
            val weight = xcmPaymentApi.queryXcmWeight(configuration.originChainId, xcmProgram)
                .getInnerSuccessOrThrow("DynamicCrossChainTransactor")

            chainRegistry.withRuntime(configuration.originChainId) {
                composeXcmExecute(xcmProgram, weight)
            }
        }
    }

    private suspend fun buildXcmProgram(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?
    ): DynamicXcmBuildResult<VersionedXcmMessage> {
        val builder = xcmBuilderFactory.createWithoutFeesMeasurement(
            initial = configuration.originChainLocation,
            xcmVersion = XcmVersion.V4
        )

        val specifiedFees = builder.buildTransferProgram(configuration, transfer, totalToWithdraw, fee)
        val program = builder.build()

        return DynamicXcmBuildResult(program, specifiedFees)
    }

    private fun XcmBuilder.buildTransferProgram(
        configuration: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?
    ): DynamicXcmSpecifiedFees {
        return when (configuration.transferType()) {
            XcmTransferReserve.TELEPORT -> buildTeleportProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                totalToWithdraw = totalToWithdraw,
                fee = fee
            )

            XcmTransferReserve.ORIGIN_RESERVE -> buildOriginReserveProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                totalToWithdraw = totalToWithdraw,
                fee = fee
            )

            XcmTransferReserve.DESTINATION_RESERVE -> buildDestinationReserveProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                totalToWithdraw = totalToWithdraw,
                fee = fee
            )

            XcmTransferReserve.REMOTE_RESERVE -> buildRemoteReserveProgram(
                assetLocation = configuration.assetAbsoluteLocation,
                remoteReserveLocation = configuration.remoteReserveChainLocation!!,
                destinationChainLocation = configuration.destinationChainLocation,
                beneficiary = transfer.recipientAccountId,
                totalToWithdraw = totalToWithdraw,
                fee = fee
            )
        }
    }

    private fun XcmBuilder.buildTeleportProgram(
        assetLocation: AbsoluteMultiLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?,
    ): DynamicXcmSpecifiedFees {
        val defaultFee = totalToWithdraw / 3.toBigInteger()

        val originChainLocation = currentLocation

        val originFees = fee.getFeeOnOrDefault(originChainLocation, defaultFee)
        val destinationFees = fee.getFeeOnOrDefault(destinationChainLocation, defaultFee)

        val remainingToDestination = totalToWithdraw - originFees

        // -------- Origin -------

        withdrawAsset(assetLocation, totalToWithdraw)
        // Here and onward: we use buy execution for the very first segment to be able to pay delivery fees in sending asset
        // WeightLimit.zero() is used since it doesn't matter anyways as the message on origin is already weighted
        buyExecution(assetLocation, originFees, WeightLimit.zero())

        val filter = determineForwardAmountFilter(fee, originChainLocation, assetLocation, remainingToDestination)
        initiateTeleport(filter, destinationChainLocation)

        // -------- Destination -------

        buyExecution(assetLocation, destinationFees, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)

        return CheckBuyExecutionIgnoredFees(buildMap {
            put(originChainLocation.chainId, originFees)
            put(destinationChainLocation.chainId, destinationFees)
        })
    }

    context(XcmBuilder)
    private fun determineForwardAmountFilter(
        fee: PostSubmissionAmountFee?,
        sourceChain: ChainLocation,
        assetLocation: AbsoluteMultiLocation,
        amountToSend: Balance
    ): MultiAssetFilter {
        val source = if (fee != null) {
            val metadata = fee.metadata as DynamicXcmFeeMetadata
            metadata.xcmFeesSourceByChain.getValue(sourceChain.chainId)
        } else {
            // Fee-loading dry runs are done with Buy Execution so we can corretly check
            XcmFeesSource.BUY_EXECUTION
        }

        return when (source) {
            XcmFeesSource.BUY_EXECUTION -> {
                val remainingToDestinationAsset = MultiAsset.from(assetLocation.relativeToLocal(), amountToSend)
                MultiAssetFilter.Definite(remainingToDestinationAsset)
            }

            XcmFeesSource.TRANSFERRING_AMOUNT -> {
                MultiAssetFilter.Wild.AllCounted(1)
            }
        }
    }

    private fun XcmBuilder.buildOriginReserveProgram(
        assetLocation: AbsoluteMultiLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?
    ): DynamicXcmSpecifiedFees {
        val defaultFee = totalToWithdraw / 3.toBigInteger()

        val originFees = fee.getFeeOnOrDefault(currentLocation, defaultFee)
        val destinationFees = fee.getFeeOnOrDefault(destinationChainLocation, defaultFee)

        val remainingToDestination = totalToWithdraw - originFees

        // -------- Origin -------

        withdrawAsset(assetLocation, totalToWithdraw)
        buyExecution(assetLocation, originFees, WeightLimit.zero())

        val remainingToDestinationAsset = MultiAsset.from(assetLocation.relativeToLocal(), remainingToDestination)
        depositReserveAsset(MultiAssetFilter.Definite(remainingToDestinationAsset), destinationChainLocation)

        // -------- Destination -------

        buyExecution(assetLocation, destinationFees, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)

        return AlwaysPaidFromBuyExecution
    }

    private fun XcmBuilder.buildDestinationReserveProgram(
        assetLocation: AbsoluteMultiLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?
    ): DynamicXcmSpecifiedFees {
        val defaultFee = totalToWithdraw / 3.toBigInteger()

        val originFees = fee.getFeeOnOrDefault(currentLocation, defaultFee)
        val destinationFees = fee.getFeeOnOrDefault(destinationChainLocation, defaultFee)

        val remainingToDestination = totalToWithdraw - originFees

        // -------- Origin -------

        withdrawAsset(assetLocation, totalToWithdraw)
        buyExecution(assetLocation, originFees, WeightLimit.zero())

        val remainingToDestinationAsset = MultiAsset.from(assetLocation.relativeToLocal(), remainingToDestination)
        initiateReserveWithdraw(MultiAssetFilter.Definite(remainingToDestinationAsset), destinationChainLocation)

        // -------- Destination -------

        buyExecution(assetLocation, destinationFees, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)

        return AlwaysPaidFromBuyExecution
    }

    private fun XcmBuilder.buildRemoteReserveProgram(
        assetLocation: AbsoluteMultiLocation,
        remoteReserveLocation: ChainLocation,
        destinationChainLocation: ChainLocation,
        beneficiary: AccountIdKey,
        totalToWithdraw: Balance,
        fee: PostSubmissionAmountFee?
    ): DynamicXcmSpecifiedFees {
        val defaultFee = totalToWithdraw / 4.toBigInteger()

        val originFees = fee.getFeeOnOrDefault(currentLocation, defaultFee)
        val reserveFees = fee.getFeeOnOrDefault(remoteReserveLocation, defaultFee)
        val destinationFees = fee.getFeeOnOrDefault(destinationChainLocation, defaultFee)

        val remainingToReserve = totalToWithdraw - originFees
        val remainingToDestination = remainingToReserve - reserveFees

        // -------- Origin -------

        withdrawAsset(assetLocation, totalToWithdraw)
        buyExecution(assetLocation, originFees, WeightLimit.zero())

        val remainingToReserveAsset = MultiAsset.from(assetLocation.relativeToLocal(), remainingToReserve)
        initiateReserveWithdraw(MultiAssetFilter.Definite(remainingToReserveAsset), remoteReserveLocation)

        // Remote reserve

        buyExecution(assetLocation, reserveFees, WeightLimit.Unlimited)

        val remainingToDestinationAsset = MultiAsset.from(assetLocation.relativeToLocal(), remainingToDestination)
        depositReserveAsset(MultiAssetFilter.Definite(remainingToDestinationAsset), destinationChainLocation)

        // -------- Destination -------

        buyExecution(assetLocation, destinationFees, WeightLimit.Unlimited)
        depositAsset(MultiAssetFilter.singleCounted(), beneficiary)

        return AlwaysPaidFromBuyExecution
    }


    private fun PostSubmissionAmountFee?.getFeeOnOrDefault(chainLocation: ChainLocation, default: Balance): Balance {
        return this?.getFeeOn(chainLocation.chainId) ?: default
    }

    private fun <T> T.versionedXcm() = versionedXcm(USED_XCM_VERSION)

    private fun AssetTransferBase.beneficiaryLocation(): RelativeMultiLocation {
        val accountId = destinationChain.accountIdOrDefault(recipient).intoKey()
        return accountId.toMultiLocation()
    }

    private object AlwaysPaidFromBuyExecution: DynamicXcmSpecifiedFees {
        override fun determineXcmFeeSource(chainId: ChainId, trappedAssets: Balance): XcmFeesSource {
            return XcmFeesSource.BUY_EXECUTION
        }
    }

    private class CheckBuyExecutionIgnoredFees(
        private val feesUsedInDryRun: Map<ChainId, Balance>
    ): DynamicXcmSpecifiedFees {

        override fun determineXcmFeeSource(chainId: ChainId, trappedAssets: Balance): XcmFeesSource {
            // BuyExecution was ignored - the number of trapped assets is exactly equal to what we put in BuyExecution
            // It cannot be more than that since the remaining part in that case will be used to pay fees and be transferred to the next hop
            return if (feesUsedInDryRun.getValue(chainId) == trappedAssets) {
                XcmFeesSource.TRANSFERRING_AMOUNT
            } else {
                XcmFeesSource.BUY_EXECUTION
            }
        }
    }
}

data class DynamicXcmBuildResult<T>(
    val value: T,
    val specifiedFees: DynamicXcmSpecifiedFees
)

inline fun <T, R> DynamicXcmBuildResult<T>.map(transform: (T) -> R): DynamicXcmBuildResult<R> {
    return DynamicXcmBuildResult(transform(value), specifiedFees)
}

interface DynamicXcmSpecifiedFees {

    fun determineXcmFeeSource(chainId: ChainId, trappedAssets: Balance): XcmFeesSource
}
