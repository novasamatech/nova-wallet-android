package io.novafoundation.nova.feature_account_impl.data.fee.types.hydra

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.ResetMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetFeesMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetMode
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.quote
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationAcceptedFeeCurrenciesFetcher
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationOraclePriceConverter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
import java.math.BigInteger
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope

internal class HydrationConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val chainRegistry: ChainRegistry,
    private val hydrationFeeInjector: HydrationFeeInjector,
    private val hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
    private val hydrationOraclePriceConverter: HydrationOraclePriceConverter,
    private val hydrationPriceConversionFallback: HydrationPriceConversionFallback,
    private val hydrationAcceptedFeeCurrenciesFetcher: HydrationAcceptedFeeCurrenciesFetcher,
    private val accountRepository: AccountRepository,
    private val coroutineScope: CoroutineScope
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        val setFeesMode = SetFeesMode(
            setMode = SetMode.Always,
            resetMode = ResetMode.ToNative
        )
        hydrationFeeInjector.setFees(extrinsicBuilder, paymentAsset, setFeesMode)
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        // The chain converts the fee with an oracle price and falls back to `AcceptedCurrencies`, never swapping.
        // A best-path quote lands on whichever pool is furthest from the market, so it is only a last resort here
        val convertedAmount = hydrationOraclePriceConverter.convertNativeAmount(nativeFee.amount, paymentAsset)
            ?: runCatching { hydrationPriceConversionFallback.convertNativeAmount(nativeFee.amount, paymentAsset) }
                .recoverCatching { quoteNativeFee(nativeFee) }
                .getOrThrow()

        return SubstrateFee(
            amount = convertedAmount,
            submissionOrigin = nativeFee.submissionOrigin,
            asset = paymentAsset
        )
    }

    private suspend fun quoteNativeFee(nativeFee: Fee): BigInteger {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val accountId = metaAccount.requireAccountIdIn(chain)
        val fromAsset = chain.commissionAsset

        val quoter = hydraDxQuoteSharedComputation.getQuoter(chain, accountId, coroutineScope)

        return quoter.findBestPath(
            chainAssetIn = fromAsset,
            chainAssetOut = paymentAsset,
            amount = nativeFee.amount,
            swapDirection = SwapDirection.SPECIFIED_IN
        ).bestPath.quote
    }
}
