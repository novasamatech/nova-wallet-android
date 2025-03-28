package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.replaceAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFee
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.calls.composeBatchAll
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.calls.composeDispatchAs
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.AmountWithdrawMode
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing.AssetIssuerRegistry
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.requireFungible
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction
import io.novafoundation.nova.feature_xcm_api.message.XcmMessage
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
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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
private const val ASSETS_TRAPPED_ARGUMENT_INDEX = 2

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
    ): CrossChainFee {
        val safeTransfer = transfer.ensureSafeAmount()

        val originResult = dryRunOnOrigin(config, safeTransfer)
        val remoteReserveResult = dryRunOnRemoteReserve(config, originResult.forwardedXcm)
        val toDestination = (remoteReserveResult ?: originResult).forwardedXcm
        val destinationResult = dryRunOnDestination(config, safeTransfer, toDestination)

        return CrossChainFee.fromDryRunResult(
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
    ): OriginDryRunResult {
        val runtime = chainRegistry.getRuntime(config.originChainId)

        val xcmResultsVersion = XcmVersion.V4
        val (dryRunCall, specifiedFees) = constructDryRunCall(config, transfer, runtime)
        val dryRunResult = dryRunApi.dryRunCall(OriginCaller.System.Root, dryRunCall, xcmResultsVersion, config.originChainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val nextHopLocation = (config.remoteReserveChainLocation ?: config.destinationChainLocation).location

        val forwardedXcm = searchForwardedXcm(
            dryRunEffects = dryRunResult,
            destination = nextHopLocation.fromPointOfViewOf(config.originChainLocation.location),
            runtimeSnapshot = runtime
        )
        val deliveryFee = searchDeliveryFee(dryRunResult, runtime)
        val trappedAssets = searchTrappedAssets(dryRunResult, runtime)

        return OriginDryRunResult(specifiedFees, forwardedXcm, deliveryFee, trappedAssets, config.originChainId)
    }

    private suspend fun dryRunOnRemoteReserve(
        config: DynamicCrossChainTransferConfiguration,
        forwardedFromOrigin: VersionedRawXcmMessage,
    ): IntermediateDryRunResult? {
        val remoteReserveLocation = config.remoteReserveChainLocation ?: return null

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
            runtimeSnapshot = runtime
        )
        val deliveryFee = searchDeliveryFee(dryRunResult, runtime)
        val trappedAssets = searchTrappedAssets(dryRunResult, runtime)

        return IntermediateDryRunResult(forwardedXcm, deliveryFee, trappedAssets, remoteReserveLocation.chainId)
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
        val dryRunResult = dryRunApi.dryRunXcm(dryRunOrigin, forwardedFromPrevious, destinationLocation.chainId)
            .getInnerSuccessOrThrow(LOG_TAG)

        val depositedAmount = searchDepositAmount(dryRunResult, transfer.destinationChainAsset)

        return FinalDryRunResult(depositedAmount, destinationLocation.chainId)
    }

    private suspend fun constructDryRunCall(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        runtime: RuntimeSnapshot
    ): DynamicXcmBuildResult<GenericCall.Instance> {
        return dynamicCrossChainTransactor.composeCrossChainTransferCall(
            configuration = config,
            transfer = transfer,
            mode = AmountWithdrawMode.SPECIFIED_EXACT,
            fee = null
        ).map { callOnOrigin ->
            val dryRunAccount = transfer.originChain.emptyAccountIdKey()
            val transferOrigin = OriginCaller.System.Signed(dryRunAccount)

            val calls = buildList {
                addFundCalls(transfer, dryRunAccount)

                val transferCall = runtime.composeDispatchAs(callOnOrigin, transferOrigin)
                add(transferCall)
            }

            runtime.composeBatchAll(calls)
        }
    }

    private suspend fun MutableList<GenericCall.Instance>.addFundCalls(transfer: AssetTransferBase, dryRunAccount: AccountIdKey) {
        val fundAmount = determineFundAmount(transfer)

        // Fund native first so we can later fund non-sufficient assets
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

    private fun CrossChainFee.Companion.fromDryRunResult(
        initialAmount: Balance,
        origin: OriginDryRunResult,
        remoteReserve: IntermediateDryRunResult?,
        destination: FinalDryRunResult
    ): CrossChainFee {
        return CrossChainFee(
            byAccount = origin.deliveryFee,
            fromAmountByChain = buildFeeMap(initialAmount, origin, remoteReserve, destination),
            metadata = buildDynamicXcmFeeMetadata(origin, remoteReserve)
        )
    }

    private fun buildDynamicXcmFeeMetadata(
        origin: OriginDryRunResult,
        remoteReserve: IntermediateDryRunResult?,
    ): DynamicXcmFeeMetadata {
        val feeValuesInDryRunCallByChain = origin.feeValuesInDryRunCall

        val xcmFeeSourceByChain = buildMap {
            val originFeesSource = feeValuesInDryRunCallByChain.determineXcmFeeSource(origin.chainId, origin.trapped)
            put(origin.chainId, originFeesSource)

            if (remoteReserve != null) {
                val reserveFeesSource = feeValuesInDryRunCallByChain.determineXcmFeeSource(remoteReserve.chainId, remoteReserve.trapped)
                put(remoteReserve.chainId, reserveFeesSource)
            }
        }

        return DynamicXcmFeeMetadata(xcmFeeSourceByChain)
    }

    private fun buildFeeMap(
        initialAmount: Balance,
        origin: IntermediateDryRunResult,
        remoteReserve: IntermediateDryRunResult?,
        destination: FinalDryRunResult
    ): Map<ChainId, Balance> {
        return buildMap {
            put(origin.chainId, origin.paidFees(initialAmount))

            if (remoteReserve != null) {
                val arrivedToReserve = origin.forwardedAmount()
                put(remoteReserve.chainId, remoteReserve.paidFees(arrivedToReserve))

                val arrivedToDestination = remoteReserve.forwardedAmount()
                put(destination.chainId, destination.paidFees(arrivedToDestination))
            } else {
                val arrivedToDestination = origin.forwardedAmount()
                put(destination.chainId, destination.paidFees(arrivedToDestination))
            }
        }
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

    private class OriginDryRunResult(
        val feeValuesInDryRunCall: DynamicXcmSpecifiedFees,
        forwardedXcm: VersionedRawXcmMessage,
        deliveryFee: Balance,
        trapped: Balance,
        chainId: ChainId,
    ) : IntermediateDryRunResult(forwardedXcm, deliveryFee, trapped, chainId)

    private open class IntermediateDryRunResult(
        val forwardedXcm: VersionedRawXcmMessage,
        val deliveryFee: Balance,
        val trapped: Balance,
        val chainId: ChainId,
    ) {

        fun forwardedAmount(): Balance {
            val message = XcmMessage.bindKnown(forwardedXcm.xcm.toEncodableInstance(), forwardedXcm.version)
            return message.instructions
                .tryFindNonNull { it.forwardedAmount() }
                .orZero()
        }

        fun paidFees(initialAmount: Balance): Balance {
            return initialAmount - trapped - forwardedAmount()
        }

        private fun XcmInstruction.forwardedAmount(): Balance? {
            return when (this) {
                is XcmInstruction.WithdrawAsset -> assets.extractFirstAmount()
                is XcmInstruction.ReserveAssetDeposited -> assets.extractFirstAmount()
                is XcmInstruction.ReceiveTeleportedAsset -> assets.extractFirstAmount()
                else -> null
            }
        }
    }

    private class FinalDryRunResult(
        val depositedAmount: Balance,
        val chainId: ChainId
    ) {

        fun paidFees(initialAmount: Balance): Balance {
            return initialAmount - depositedAmount
        }
    }
}


private fun MultiAssets.extractFirstAmount(): Balance {
    return if (value.isNotEmpty()) {
        value.first().requireFungible().amount
    } else {
        Balance.ZERO
    }
}
