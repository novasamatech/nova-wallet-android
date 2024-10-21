package io.novafoundation.nova.feature_account_impl.data.fee.types.hydra

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

internal class RealHydrationFeeInjector(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydrationFeeInjector {

    override suspend fun setFees(
        extrinsicBuilder: ExtrinsicBuilder,
        paymentAsset: Chain.Asset,
        mode: HydrationFeeInjector.SetFeesMode
    ) {
        val baseCalls = extrinsicBuilder.getCalls()
        extrinsicBuilder.resetCalls()

        val justSetFees = getSetPhase(mode.setMode).setFees(extrinsicBuilder, paymentAsset)
        extrinsicBuilder.calls(baseCalls)
        getResetPhase(mode.resetMode).resetFees(extrinsicBuilder, justSetFees)
    }

    private fun getSetPhase(mode: HydrationFeeInjector.SetMode): SetPhase {
        return when(mode) {
            HydrationFeeInjector.SetMode.Always -> AlwaysSetPhase()
            is HydrationFeeInjector.SetMode.Lazy -> LazySetPhase(mode.currentlySetFeeAsset)
        }
    }

    private fun getResetPhase(mode: HydrationFeeInjector.ResetMode): ResetPhase {
        return when(mode) {
            HydrationFeeInjector.ResetMode.ToNative -> AlwaysResetPhase()
            is HydrationFeeInjector.ResetMode.ToNativeLazily -> LazyResetPhase(mode.feeAssetBeforeTransaction)
        }
    }

    private interface SetPhase {

        /**
         * @return just set on-chain asset id, if changed
         */
        suspend fun setFees(extrinsicBuilder: ExtrinsicBuilder, paymentAsset: Chain.Asset): HydraDxAssetId?
    }

    private interface ResetPhase {

        suspend fun resetFees(
            extrinsicBuilder: ExtrinsicBuilder,
            feesModifiedInSetPhase: HydraDxAssetId?
        )
    }

    private inner class AlwaysSetPhase : SetPhase {

        override suspend fun setFees(extrinsicBuilder: ExtrinsicBuilder, paymentAsset: Chain.Asset): HydraDxAssetId {
            val onChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(paymentAsset)
            extrinsicBuilder.setFeeCurrency(onChainId)
            return onChainId
        }
    }

    private inner class LazySetPhase(
        private val currentFeeTokenId: HydraDxAssetId,
    ) : SetPhase {

        override suspend fun setFees(extrinsicBuilder: ExtrinsicBuilder, paymentAsset: Chain.Asset): HydraDxAssetId? {
            val paymentCurrencyToSet = getPaymentCurrencyToSetIfNeeded(paymentAsset)

            paymentCurrencyToSet?.let {
                extrinsicBuilder.setFeeCurrency(paymentCurrencyToSet)
            }

            return paymentCurrencyToSet
        }

        private suspend fun getPaymentCurrencyToSetIfNeeded(expectedPaymentAsset: Chain.Asset): HydraDxAssetId? {
            val expectedPaymentTokenId = hydraDxAssetIdConverter.toOnChainIdOrThrow(expectedPaymentAsset)

            return expectedPaymentTokenId.takeIf { currentFeeTokenId != expectedPaymentTokenId }
        }
    }

    private inner class AlwaysResetPhase : ResetPhase {

        override suspend fun resetFees(
            extrinsicBuilder: ExtrinsicBuilder,
            feesModifiedInSetPhase: HydraDxAssetId?
        ) {
            extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
        }
    }

    private inner class LazyResetPhase(
        private val previousFeeCurrency: HydraDxAssetId
    ) : ResetPhase {

        override suspend fun resetFees(extrinsicBuilder: ExtrinsicBuilder, feesModifiedInSetPhase: HydraDxAssetId?) {
            val justSetFeeToNonNative = feesModifiedInSetPhase != null && feesModifiedInSetPhase != hydraDxAssetIdConverter.systemAssetId
            val previousCurrencyRemainsNonNative = feesModifiedInSetPhase == null && previousFeeCurrency != hydraDxAssetIdConverter.systemAssetId

            if (justSetFeeToNonNative || previousCurrencyRemainsNonNative) {
                extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
            }
        }
    }

    private fun ExtrinsicBuilder.setFeeCurrency(onChainId: HydraDxAssetId) {
        call(
            moduleName = Modules.MULTI_TRANSACTION_PAYMENT,
            callName = "set_currency",
            arguments = mapOf(
                "currency" to onChainId
            )
        )
    }
}
