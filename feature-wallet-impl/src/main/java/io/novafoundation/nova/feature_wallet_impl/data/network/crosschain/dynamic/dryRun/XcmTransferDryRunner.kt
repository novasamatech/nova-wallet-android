package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.isRemoteReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.remoteReserveLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.originChainId
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.calls.composeBatchAll
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.calls.composeDispatchAs
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainTransactor
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunResult.FinalSegment
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunResult.IntermediateSegment
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing.AssetIssuerRegistry
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.requireFungible
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.DryRunEffects
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.getByLocation
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.senderXcmVersion
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
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import javax.inject.Inject

interface XcmTransferDryRunner {

    suspend fun dryRunXcmTransfer(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        origin: XcmTransferDryRunOrigin
    ): Result<XcmTransferDryRunResult>
}

@FeatureScope
class RealXcmTransferDryRunner @Inject constructor(
    private val dryRunApi: DryRunApi,
    private val chainRegistry: ChainRegistry,
    private val assetIssuerRegistry: AssetIssuerRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val dynamicCrossChainTransactor: DynamicCrossChainTransactor,
) : XcmTransferDryRunner {

    companion object {

        private const val MINIMUM_FUND_AMOUNT = 100

        private const val FEES_PAID_FEES_ARGUMENT_INDEX = 1
        private const val ASSETS_TRAPPED_ARGUMENT_INDEX = 2
    }

    override suspend fun dryRunXcmTransfer(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        origin: XcmTransferDryRunOrigin
    ): Result<XcmTransferDryRunResult> {
        return runCatching {
            val originResult = dryRunOnOrigin(config, transfer, origin)
            val remoteReserveResult = dryRunOnRemoteReserve(config, originResult.forwardedXcm)
            val destinationResult = dryRunOnDestination(config, transfer, remoteReserveResult.forwardedXcm)

            XcmTransferDryRunResult(
                origin = originResult.toPublicResult(),
                remoteReserve = remoteReserveResult.takeIfRemoteReserve(config)?.toPublicResult(),
                destination = destinationResult.toPublicResult()
            )
        }
            .onFailure { Log.w(LOG_TAG, "Dry run failed", it) }
    }

    private suspend fun dryRunOnOrigin(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        origin: XcmTransferDryRunOrigin,
    ): IntermediateDryRunResult {
        val runtime = chainRegistry.getRuntime(config.originChainId)
        val xcmResultsVersion = XcmVersion.V4

        val (dryRunCall, dryRunOrigin) = constructDryRunCallParams(config, transfer, origin, runtime)
        val dryRunResult = dryRunApi.dryRunCall(dryRunOrigin, dryRunCall, xcmResultsVersion, config.originChainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val nextHopLocation = (config.transferType.remoteReserveLocation() ?: config.destinationChainLocation).location

        val forwardedXcm = searchForwardedXcm(
            dryRunEffects = dryRunResult,
            destination = nextHopLocation.fromPointOfViewOf(config.originChainLocation.location),
        )
        val deliveryFee = searchDeliveryFee(dryRunResult, runtime)
        val trappedAssets = searchTrappedAssets(dryRunResult, runtime)

        return IntermediateDryRunResult(forwardedXcm, deliveryFee, trappedAssets)
    }

    private suspend fun constructDryRunCallParams(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        origin: XcmTransferDryRunOrigin,
        runtime: RuntimeSnapshot
    ): OriginCallParams {
        return when (origin) {
            XcmTransferDryRunOrigin.Fake -> constructDryRunCallFromFakeOrigin(transfer, config, runtime)

            is XcmTransferDryRunOrigin.Signed -> constructDryRunCallFromRealOrigin(transfer, config, origin)
        }
    }

    private suspend fun constructDryRunCallFromRealOrigin(
        transfer: AssetTransferBase,
        config: DynamicCrossChainTransferConfiguration,
        origin: XcmTransferDryRunOrigin.Signed,
    ): OriginCallParams {
        val callOnOrigin = dynamicCrossChainTransactor.composeCrossChainTransferCall(config, transfer, crossChainFee = origin.crossChainFee)

        return OriginCallParams(
            call = callOnOrigin,
            origin = OriginCaller.System.Signed(origin.accountId)
        )
    }

    private suspend fun constructDryRunCallFromFakeOrigin(
        transfer: AssetTransferBase,
        config: DynamicCrossChainTransferConfiguration,
        runtime: RuntimeSnapshot,
    ): OriginCallParams {
        val callOnOrigin = dynamicCrossChainTransactor.composeCrossChainTransferCall(config, transfer, crossChainFee = Balance.ZERO)

        val dryRunAccount = transfer.originChain.emptyAccountIdKey()
        val transferOrigin = OriginCaller.System.Signed(dryRunAccount)

        val calls = buildList {
            addFundCalls(transfer, dryRunAccount)

            val transferCallFromOrigin = runtime.composeDispatchAs(callOnOrigin, transferOrigin)
            add(transferCallFromOrigin)
        }

        val finalOriginCall = runtime.composeBatchAll(calls)
        return OriginCallParams(finalOriginCall, OriginCaller.System.Root)
    }

    private suspend fun MutableList<GenericCall.Instance>.addFundCalls(transfer: AssetTransferBase, dryRunAccount: AccountIdKey) {
        val fundAmount = determineFundAmount(transfer)

        // Fund native asset first so we can later fund potentially non-sufficient assets
        if (!transfer.originChainAsset.isUtilityAsset) {
            // Additionally fund native asset to pay delivery fees
            val nativeAsset = transfer.originChain.utilityAsset
            val planks = nativeAsset.planksFromAmount(MINIMUM_FUND_AMOUNT.toBigDecimal())
            val fundNativeAssetCall = assetIssuerRegistry.create(nativeAsset).composeIssueCall(planks, dryRunAccount)
            add(fundNativeAssetCall)
        }

        val fundSendingAssetCall = assetIssuerRegistry.create(transfer.originChainAsset).composeIssueCall(fundAmount, dryRunAccount)
        add(fundSendingAssetCall)
    }

    private suspend fun dryRunOnRemoteReserve(
        config: DynamicCrossChainTransferConfiguration,
        forwardedFromOrigin: VersionedRawXcmMessage,
    ): IntermediateDryRunResult {
        // No remote reserve - nothing to dry run, return unchanged value
        val remoteReserveLocation = config.transferType.remoteReserveLocation()
            ?: return IntermediateDryRunResult(forwardedFromOrigin, Balance.ZERO, Balance.ZERO)

        val runtime = chainRegistry.getRuntime(remoteReserveLocation.chainId)

        val originLocation = config.originChainLocation.location
        val destinationLocation = config.destinationChainLocation.location

        val usedXcmVersion = forwardedFromOrigin.version

        val dryRunOrigin = originLocation.fromPointOfViewOf(remoteReserveLocation.location).versionedXcm(usedXcmVersion)
        val dryRunResult = dryRunApi.dryRunXcm(dryRunOrigin, forwardedFromOrigin, remoteReserveLocation.chainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val destinationOnRemoteReserve = destinationLocation.fromPointOfViewOf(remoteReserveLocation.location)

        val forwardedXcm = searchForwardedXcm(
            dryRunEffects = dryRunResult,
            destination = destinationOnRemoteReserve,
        )
        val deliveryFee = searchDeliveryFee(dryRunResult, runtime)
        val trappedAssets = searchTrappedAssets(dryRunResult, runtime)

        return IntermediateDryRunResult(forwardedXcm, deliveryFee, trappedAssets)
    }

    private suspend fun dryRunOnDestination(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        forwardedFromPrevious: VersionedRawXcmMessage,
    ): FinalDryRunResult {
        val previousLocation = (config.transferType.remoteReserveLocation() ?: config.originChainLocation).location
        val destinationLocation = config.destinationChainLocation

        val usedXcmVersion = forwardedFromPrevious.version

        val dryRunOrigin = previousLocation.fromPointOfViewOf(destinationLocation.location).versionedXcm(usedXcmVersion)
        val dryRunResult = dryRunApi.dryRunXcm(dryRunOrigin, forwardedFromPrevious, destinationLocation.chainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val depositedAmount = searchDepositAmount(dryRunResult, transfer.destinationChainAsset, transfer.recipientAccountId)

        return FinalDryRunResult(depositedAmount)
    }

    private fun searchForwardedXcm(
        dryRunEffects: DryRunEffects,
        destination: RelativeMultiLocation,
    ): VersionedRawXcmMessage {
        return searchForwardedXcmInQueues(dryRunEffects, destination)
    }

    private suspend fun searchDepositAmount(
        dryRunEffects: DryRunEffects,
        chainAsset: Chain.Asset,
        recipientAccountId: AccountIdKey,
    ): Balance {
        val depositDetector = assetSourceRegistry.getEventDetector(chainAsset)

        val deposits = dryRunEffects.emittedEvents.mapNotNull { depositDetector.detectDeposit(it) }
            .filter { it.destination == recipientAccountId }

        if (deposits.isEmpty()) error("No deposits detected")

        return deposits.sumOf { it.amount }
    }

    private fun searchDeliveryFee(
        dryRunEffects: DryRunEffects,
        runtimeSnapshot: RuntimeSnapshot,
    ): Balance {
        val xcmPalletName = runtimeSnapshot.metadata.xcmPalletName()
        val event = dryRunEffects.emittedEvents.findEvent(xcmPalletName, "FeesPaid") ?: return Balance.ZERO

        val usedXcmVersion = dryRunEffects.senderXcmVersion()

        val feesDecoded = event.arguments[FEES_PAID_FEES_ARGUMENT_INDEX]
        val multiAssets = MultiAssets.bind(feesDecoded, usedXcmVersion)

        return multiAssets.extractFirstAmount()
    }

    private fun searchTrappedAssets(
        dryRunEffects: DryRunEffects,
        runtimeSnapshot: RuntimeSnapshot,
    ): Balance {
        val xcmPalletName = runtimeSnapshot.metadata.xcmPalletName()
        val event = dryRunEffects.emittedEvents.findEvent(xcmPalletName, "AssetsTrapped") ?: return Balance.ZERO

        val feesDecoded = event.arguments[ASSETS_TRAPPED_ARGUMENT_INDEX]
        val multiAssets = MultiAssets.bindVersioned(feesDecoded).xcm

        return multiAssets.extractFirstAmount()
    }

    private fun MultiAssets.extractFirstAmount(): Balance {
        return if (value.isNotEmpty()) {
            value.first().requireFungible().amount
        } else {
            Balance.ZERO
        }
    }

    private fun searchForwardedXcmInQueues(
        dryRunEffects: DryRunEffects,
        destination: RelativeMultiLocation
    ): VersionedRawXcmMessage {
        val usedXcmVersion = dryRunEffects.senderXcmVersion()
        val versionedDestination = destination.versionedXcm(usedXcmVersion)

        val forwardedXcmsToDestination = dryRunEffects.forwardedXcms.getByLocation(versionedDestination)

        // There should only be one forwarded message during dry run
        return forwardedXcmsToDestination.first()
    }

    private fun determineFundAmount(transfer: AssetTransferBase): Balance {
        val amount = (transfer.amount() * 2.toBigDecimal()).coerceAtLeast(MINIMUM_FUND_AMOUNT.toBigDecimal())
        return transfer.originChainAsset.planksFromAmount(amount)
    }

    private fun IntermediateDryRunResult.toPublicResult(): IntermediateSegment {
        return IntermediateSegment(
            deliveryFee = deliveryFee,
            trapped = trapped
        )
    }

    private fun FinalDryRunResult.toPublicResult(): FinalSegment {
        return FinalSegment(depositedAmount = depositedAmount)
    }

    private fun IntermediateDryRunResult.takeIfRemoteReserve(config: DynamicCrossChainTransferConfiguration): IntermediateDryRunResult? {
        return takeIf { config.transferType.isRemoteReserve() }
    }

    private class IntermediateDryRunResult(
        val forwardedXcm: VersionedRawXcmMessage,
        val deliveryFee: Balance,
        val trapped: Balance,
    )

    private class FinalDryRunResult(
        val depositedAmount: Balance
    )

    private data class OriginCallParams(val call: GenericCall.Instance, val origin: OriginCaller)
}
