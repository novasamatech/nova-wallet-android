package io.novafoundation.nova.feature_xcm_impl.builder.fees

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.composeBatchAll
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.planksFromAmount
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.intoMultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.requireFungible
import io.novafoundation.nova.feature_xcm_api.builder.fees.MeasureXcmFees
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.XcmAssetIssuer
import io.novafoundation.nova.feature_xcm_api.extrinsic.composeDispatchAs
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction.WithdrawAsset
import io.novafoundation.nova.feature_xcm_api.message.findInstruction
import io.novafoundation.nova.feature_xcm_api.message.modifyInstruction
import io.novafoundation.nova.feature_xcm_api.message.modifyInstructionOrPrepend
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.multiAssetIdOn
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.CallDryRunEffects
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.runtimeApi.getInnerSuccessOrThrow
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.versions.bindVersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.runtime.ext.emptyAccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall


private const val FEE_AMOUNT = 100
private const val MINIMUM_WITHDRAW_AMOUNT = FEE_AMOUNT * 100 // enough to pay for 100 different hops
private const val MINIMUM_FUND_AMOUNT = MINIMUM_WITHDRAW_AMOUNT * 2

private const val WEIGHT_BUGGER = 1.1

private const val TRAPPED_ASSETS_ARG_INDEX = 2

class DryRunMeasuresXcmFees(
    private val dryRunApi: DryRunApi,
    private val xcmPaymentApi: XcmPaymentApi,
    private val chainRegistry: ChainRegistry,
    private val assetIssuer: XcmAssetIssuer,
) : MeasureXcmFees {

    override suspend fun measureFees(
        message: VersionedXcmMessage,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation
    ): BalanceOf {
        val chain = chainRegistry.getChain(chainLocation.chainId)
        val dryRunAccountId = chain.emptyAccountIdKey()

        val dryRunReadyMessage = modifyXcmMessageForDryRun(message, feeAsset, chainLocation)
        val dryRunCall = composeDryRunCall(dryRunReadyMessage, feeAsset, chainLocation, dryRunAccountId)

        val dryRunResult = dryRunApi.dryRunCall(
            originCaller = OriginCaller.System.Root,
            call = dryRunCall,
            resultsXcmVersion = message.version,
            chainId = chainLocation.chainId
        ).getInnerSuccessOrThrow("DryRunMeasuresXcmFees")

        return determineActualFee(dryRunResult, feeAsset, chainLocation)
    }

    private suspend fun determineActualFee(
        callDryRunEffects: CallDryRunEffects,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation
    ): BalanceOf {
        val feeOverEstimate = chainRegistry.withRuntime(chainLocation.chainId) {
            val trapEvent = callDryRunEffects.emittedEvents.findEvent(metadata.xcmPalletName(), "AssetsTrapped") ?: return BalanceOf.ZERO
            val trappedAssetsArg = trapEvent.arguments[TRAPPED_ASSETS_ARG_INDEX]
            val trappedAssets = bindVersionedXcm(trappedAssetsArg, MultiAssets::bind).xcm

            trappedAssets.value.first().requireFungible().amount
        }

        val feeChainAsset = chainRegistry.asset(feeAsset.assetId)
        val feeLimit = FEE_AMOUNT.toPlanks(feeChainAsset)

        return (feeLimit - feeOverEstimate).atLeastZero()
    }

    private suspend fun composeDryRunCall(
        message: VersionedXcmMessage,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation,
        dryRunAccountId: AccountIdKey
    ): GenericCall.Instance {
        val xcmExecuteCall = composeXcmExecuteCall(message, chainLocation)
        val fundCall = composeFundCall(message, feeAsset, chainLocation, dryRunAccountId)

        return chainRegistry.withRuntime(chainLocation.chainId) {
            val dryRunAccountOrigin = OriginCaller.System.Signed(dryRunAccountId)
            val xcmExecuteFromDryRunAccount = runtime.composeDispatchAs(xcmExecuteCall, dryRunAccountOrigin)

            runtime.composeBatchAll(listOf(fundCall, xcmExecuteFromDryRunAccount))
        }
    }

    private suspend fun composeXcmExecuteCall(
        message: VersionedXcmMessage,
        chainLocation: ChainLocation
    ): GenericCall.Instance {
        val weight = xcmPaymentApi.queryXcmWeight(chainLocation.chainId, message)
            .getInnerSuccessOrThrow("DryRunMeasuresXcmFees")
        val weightWithBuffer = weight * WEIGHT_BUGGER

        return chainRegistry.withRuntime(chainLocation.chainId) {
            runtime.composeXcmExecute(message, weightWithBuffer)
        }
    }

    private suspend fun composeFundCall(
        message: VersionedXcmMessage,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation,
        dryRunAccountId: AccountIdKey
    ): GenericCall.Instance {
        val withdrawAmount = message.detectWithdrawAmount(feeAsset, chainLocation)

        val feeChainAsset = chainRegistry.asset(feeAsset.assetId)

        val minimumFundAmount = MINIMUM_FUND_AMOUNT.toPlanks(feeChainAsset)
        val fundAmount = minimumFundAmount.coerceAtLeast(withdrawAmount * 2.toBigInteger())

        return assetIssuer.issueAssetsCall(feeChainAsset, fundAmount, dryRunAccountId)
    }

    private fun RuntimeSnapshot.composeXcmExecute(
        message: VersionedXcmMessage,
        maxWeight: WeightV2
    ): GenericCall.Instance {
        return composeCall(
            moduleName = metadata.xcmPalletName(),
            callName = "execute",
            args = mapOf(
                "message" to message.toEncodableInstance(),
                "max_weight" to maxWeight.toEncodableInstance()
            )
        )
    }

    @Suppress("IfThenToElvis")
    private suspend fun modifyXcmMessageForDryRun(
        message: VersionedXcmMessage,
        feeAsset: AssetLocation,
        chainLocation: ChainLocation
    ): VersionedXcmMessage {
        val feeAssetId = feeAsset.multiAssetIdOn(chainLocation)
        val feeChainAsset = chainRegistry.asset(feeAsset.assetId)

        return message
            // Replace WithdrawAsset so holding register can cover PayFees
            .modifyInstructionOrPrepend<WithdrawAsset> {
                val minimumWithdrawAmount = MINIMUM_WITHDRAW_AMOUNT.toPlanks(feeChainAsset)
                val newAssets = if (it != null) {
                    it.assets.replaceAmountOf(feeAssetId) { oldAmount -> minimumWithdrawAmount + oldAmount }
                } else {
                    MultiAsset.from(feeAssetId, minimumWithdrawAmount).intoMultiAssets()
                }

                WithdrawAsset(newAssets)
            }
            // Replace PayFees with an enormous overestimate succeeds to make sure dry run always succeeds
            .modifyInstruction<XcmInstruction.PayFees> {
                val newFees = MultiAsset.from(feeAssetId, FEE_AMOUNT.toPlanks(feeChainAsset))
                XcmInstruction.PayFees(newFees)
            }
    }

    private suspend fun VersionedXcmMessage.detectWithdrawAmount(
        feeAsset: AssetLocation,
        chainLocation: ChainLocation
    ): BalanceOf {
        val feeAssetId = feeAsset.multiAssetIdOn(chainLocation)
        val feeChainAsset = chainRegistry.asset(feeAsset.assetId)

        val minimumWithdrawAmount = MINIMUM_WITHDRAW_AMOUNT.toPlanks(feeChainAsset)

        val withdrawInstruction = findInstruction<WithdrawAsset>() ?: return minimumWithdrawAmount
        val asset = withdrawInstruction.assets.value.find { it.id == feeAssetId } ?: return minimumWithdrawAmount

        return asset.requireFungible().amount
    }

    private fun Int.toPlanks(asset: Chain.Asset): BalanceOf {
        return toBigDecimal().planksFromAmount(asset.precision)
    }

    private fun MultiAssets.replaceAmountOf(assetId: MultiAssetId, transform: (oldAmount: BalanceOf) -> BalanceOf): MultiAssets {
        return value.map { multiAsset ->
            if (multiAsset.id == assetId) {
                MultiAsset.from(assetId, transform(multiAsset.requireFungible().amount))
            } else {
                multiAsset
            }
        }.intoMultiAssets()
    }
}
