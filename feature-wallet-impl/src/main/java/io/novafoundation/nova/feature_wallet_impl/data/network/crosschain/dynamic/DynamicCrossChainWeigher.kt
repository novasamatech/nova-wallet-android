package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.composeBatchAll
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.replaceAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.dryRun.AssetIssuerRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.requireFungible
import io.novafoundation.nova.feature_xcm_api.extrinsic.composeDispatchAs
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.bindRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.DryRunEffects
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.getByLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.usedXcmVersion
import io.novafoundation.nova.feature_xcm_api.runtimeApi.getInnerSuccessOrThrow
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.runtime.ext.emptyAccountIdKey
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import javax.inject.Inject

private const val MINIMUM_SEND_AMOUNT = 100
private const val MINIMUM_FUND_AMOUNT = MINIMUM_SEND_AMOUNT * 2

private const val FEES_PAID_FEES_ARGUMENT_INDEX = 1
private const val XCM_SENT_MESSAGE_ARGUMENT_INDEX = 2

@FeatureScope
class DynamicCrossChainWeigher @Inject constructor(
    private val dryRunApi: DryRunApi,
    private val dynamicCrossChainTransactor: DynamicCrossChainTransactor,
    private val chainRegistry: ChainRegistry,
    private val assetIssuerRegistry: AssetIssuerRegistry,
    private val assetSourceRegistry: AssetSourceRegistry
) {

    suspend fun estimateFee(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase
    ): CrossChainFeeModel {
        val safeTransfer = transfer.ensureSafeAmount()

        val runtime = chainRegistry.getRuntime(config.originChainId)

        val originResult = dryRunOnOrigin(config, safeTransfer, runtime)
        val remoteReserveResult = dryRunOnRemoteReserve(config, originResult.forwardedXcm, runtime)
        val destinationResult = dryRunOnDestination(config, safeTransfer, remoteReserveResult.forwardedXcm)

        return CrossChainFeeModel.fromDryRunResult(
            initialAmount = safeTransfer.amountPlanks,
            origin = originResult,
            remoteReserve = remoteReserveResult,
            destination = destinationResult
        )
    }

    // Ensure we can calculate fee regardless of what user entered
    private fun AssetTransferBase.ensureSafeAmount(): AssetTransferBase {
        val minimumSendAmount = destinationChainAsset.planksFromAmount(MINIMUM_SEND_AMOUNT.toBigDecimal())
        val safeAmount = amountPlanks.coerceAtLeast(minimumSendAmount)
        return replaceAmount(newAmount = safeAmount)
    }

    private suspend fun dryRunOnOrigin(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        runtime: RuntimeSnapshot
    ): IntermediateDryRunResult {
        val xcmVersion = XcmVersion.V4
        val dryRunCall = constructDryRunCall(config, transfer, runtime)
        val dryRunResult = dryRunApi.dryRunCall(OriginCaller.System.Root, dryRunCall, xcmVersion, config.originChainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val nextHopLocation = (config.remoteReserveChainLocation ?: config.destinationChainLocation).location

        val forwardedXcm = searchForwardedXcm(
            dryRunEffects = dryRunResult,
            destination = nextHopLocation.fromPointOfViewOf(config.originChainLocation.location),
            runtimeSnapshot = runtime
        )
        val deliveryFee = searchDeliveryFee(dryRunResult, runtime)

        return IntermediateDryRunResult(forwardedXcm, deliveryFee)
    }

    private suspend fun dryRunOnRemoteReserve(
        config: DynamicCrossChainTransferConfiguration,
        forwardedFromOrigin: VersionedRawXcmMessage,
        runtime: RuntimeSnapshot
    ): IntermediateDryRunResult {
        // No remote reserve - nothing to dry run, return unchanged value
        val remoteReserveLocation = config.remoteReserveChainLocation
            ?: return IntermediateDryRunResult(forwardedFromOrigin, Balance.ZERO)

        val originLocation = config.originChainLocation.location
        val destinationLocation = config.destinationChainLocation.location

        val usedXcmVersion = forwardedFromOrigin.version

        val dryRunOrigin = originLocation.fromPointOfViewOf(remoteReserveLocation.location).versionedXcm(usedXcmVersion)
        val dryRunResult = dryRunApi.dryRunRawXcm(dryRunOrigin, forwardedFromOrigin, remoteReserveLocation.chainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val destinationOnRemoteReserve = destinationLocation.fromPointOfViewOf(remoteReserveLocation.location)

        val forwardedXcm = searchForwardedXcm(
            dryRunEffects = dryRunResult,
            destination = destinationOnRemoteReserve,
            runtimeSnapshot = runtime
        )
        val deliveryFee = searchDeliveryFee(dryRunResult, runtime)

        return IntermediateDryRunResult(forwardedXcm, deliveryFee)
    }

    private suspend fun dryRunOnDestination(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        forwardedFromPrevious: VersionedRawXcmMessage,
    ): FinalDryRunResult {
        val previousLocation = (config.remoteReserveChainLocation ?: config.originChainLocation).location
        val destinationLocation = config.destinationChainLocation

        val usedXcmVersion = forwardedFromPrevious.version

        val dryRunOrigin = previousLocation.fromPointOfViewOf(destinationLocation.location).versionedXcm(usedXcmVersion)
        val dryRunResult = dryRunApi.dryRunRawXcm(dryRunOrigin, forwardedFromPrevious, destinationLocation.chainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val depositedAmount = searchDepositAmount(dryRunResult, transfer.destinationChainAsset)

        return FinalDryRunResult(depositedAmount)
    }

    private suspend fun constructDryRunCall(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        runtime: RuntimeSnapshot
    ): GenericCall.Instance {
        val callOnOrigin = dynamicCrossChainTransactor.composeCrossChainTransferCall(config, transfer, crossChainFee = Balance.ZERO)

        val dryRunAccount = transfer.originChain.emptyAccountIdKey()
        val transferOrigin = OriginCaller.System.Signed(dryRunAccount)

        val calls = buildList {
            addFundCalls(transfer, dryRunAccount)

            val transferCall = runtime.composeDispatchAs(callOnOrigin, transferOrigin)
            add(transferCall)
        }

        return runtime.composeBatchAll(calls)
    }

    private suspend fun MutableList<GenericCall.Instance>.addFundCalls(transfer: AssetTransferBase, dryRunAccount: AccountIdKey) {
        val fundAmount = determineFundAmount(transfer)

        val fundSendingAssetCall = assetIssuerRegistry.create(transfer.originChainAsset).composeIssueCall(fundAmount, dryRunAccount)
        add(fundSendingAssetCall)

        if (!transfer.originChainAsset.isUtilityAsset) {
            // Additionally fund native asset to pay delivery fees
            val nativeAsset = transfer.originChain.utilityAsset
            val planks = nativeAsset.planksFromAmount(MINIMUM_FUND_AMOUNT.toBigDecimal())
            val fundNativeAssetCall = assetIssuerRegistry.create(nativeAsset).composeIssueCall(planks, dryRunAccount)
            add(fundNativeAssetCall)
        }
    }

    private fun CrossChainFeeModel.Companion.fromDryRunResult(
        initialAmount: Balance,
        origin: IntermediateDryRunResult,
        remoteReserve: IntermediateDryRunResult,
        destination: FinalDryRunResult
    ): CrossChainFeeModel {
        val deliveryFee = origin.deliveryFee + remoteReserve.deliveryFee
        val totalFee = initialAmount - destination.depositedAmount
        // We do not subtract `origin.deliveryFee` since it is paid directly from the origin account and not from the holding register
        val executionFee = totalFee - remoteReserve.deliveryFee

        return CrossChainFeeModel(deliveryFee, executionFee)
    }

    // Maximum between amount * 2 and MINIMUM_FUND_AMOUNT
    private fun determineFundAmount(transfer: AssetTransferBase): Balance {
        val amount = (transfer.amount() * 2.toBigDecimal()).coerceAtLeast(MINIMUM_FUND_AMOUNT.toBigDecimal())
        return transfer.originChainAsset.planksFromAmount(amount)
    }

    private fun searchForwardedXcm(
        dryRunEffects: DryRunEffects,
        destination: RelativeMultiLocation,
        runtimeSnapshot: RuntimeSnapshot,
    ): VersionedRawXcmMessage {
        return searchForwardedXcmInEvents(dryRunEffects, runtimeSnapshot)
            ?: searchForwardedXcmInQueues(dryRunEffects, destination)
    }

    private suspend fun searchDepositAmount(dryRunEffects: DryRunEffects, chainAsset: Chain.Asset): Balance {
        val depositDetector = assetSourceRegistry.getEventDetector(chainAsset)

        val deposit = dryRunEffects.emittedEvents.tryFindNonNull {
            depositDetector.detectDeposit(it)
        }

        return deposit?.amount ?: error("No deposit detected")
    }

    private fun searchDeliveryFee(
        dryRunEffects: DryRunEffects,
        runtimeSnapshot: RuntimeSnapshot,
    ): Balance {
        val xcmPalletName = runtimeSnapshot.metadata.xcmPalletName()
        val event = dryRunEffects.emittedEvents.findEvent(xcmPalletName, "FeesPaid") ?: return Balance.ZERO

        val usedXcmVersion = dryRunEffects.usedXcmVersion()

        val feesDecoded = event.arguments[FEES_PAID_FEES_ARGUMENT_INDEX]
        val multiAssets = MultiAssets.bind(feesDecoded, usedXcmVersion).value

        return if (multiAssets.isNotEmpty()) {
            multiAssets.first().requireFungible().amount
        } else {
            Balance.ZERO
        }
    }

    private fun searchForwardedXcmInEvents(
        dryRunEffects: DryRunEffects,
        runtimeSnapshot: RuntimeSnapshot,
    ): VersionedRawXcmMessage? {
        val xcmPalletName = runtimeSnapshot.metadata.xcmPalletName()
        val event = dryRunEffects.emittedEvents.findEvent(xcmPalletName, "Sent") ?: return null
        val rawXcmMessage = bindRawXcmMessage(event.arguments[XCM_SENT_MESSAGE_ARGUMENT_INDEX])

        return rawXcmMessage.versionedXcm(dryRunEffects.usedXcmVersion())
    }

    private fun searchForwardedXcmInQueues(
        dryRunEffects: DryRunEffects,
        destination: RelativeMultiLocation
    ): VersionedRawXcmMessage {
        val usedXcmVersion = dryRunEffects.usedXcmVersion()
        val versionedDestination = destination.versionedXcm(usedXcmVersion)

        val forwardedXcmsToDestination = dryRunEffects.forwardedXcms.getByLocation(versionedDestination)

        // There should only be one forwarded message during dry run
        return forwardedXcmsToDestination.first()
    }

    private class IntermediateDryRunResult(
        val forwardedXcm: VersionedRawXcmMessage,
        val deliveryFee: Balance
    )

    private class FinalDryRunResult(
        val depositedAmount: Balance
    )
}
