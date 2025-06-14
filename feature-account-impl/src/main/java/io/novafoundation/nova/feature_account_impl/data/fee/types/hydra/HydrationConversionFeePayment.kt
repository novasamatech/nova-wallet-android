package io.novafoundation.nova.feature_account_impl.data.fee.types.hydra

import android.util.Log
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.times
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.ResetMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetFeesMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetMode
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.quote
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope

private const val FEE_QUOTE_BUFFER = 1.1

internal class HydrationConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val chainRegistry: ChainRegistry,
    private val hydrationFeeInjector: HydrationFeeInjector,
    private val hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
    private val hydrationPriceConversionFallback: HydrationPriceConversionFallback,
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
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val accountId = metaAccount.requireAccountIdIn(chain)
        val nativeAsset = chain.commissionAsset

        val quoter = hydraDxQuoteSharedComputation.getQuoter(chain, accountId, coroutineScope)

        Log.d("HydrationConversionFeePayment", "Native fee: ${nativeFee.decimalAmount}")

        val convertedAmount = runCatching {
            quoter.findBestPath(
                chainAssetIn = paymentAsset,
                chainAssetOut = nativeAsset,
                amount = nativeFee.amount,
                swapDirection = SwapDirection.SPECIFIED_OUT
            ).bestPath.quote
        }
            .recoverCatching {
                Log.w("HydrationConversionFeePayment", "Failed to quote real price for ${paymentAsset.symbol} on ${chain.name}, using fallback")

                hydrationPriceConversionFallback.convertNativeAmount(nativeFee.amount, paymentAsset)
            }
            .getOrThrow()

        Log.d("HydrationConversionFeePayment", "Converted fee: ${convertedAmount.amountFromPlanks(paymentAsset.precision)}")

        // Fees in non-native assets are especially volatile since conversion happens through swaps so we add some buffer to mitigate volatility
        val convertedAmountWithBuffer = convertedAmount * FEE_QUOTE_BUFFER

        Log.d("HydrationConversionFeePayment", "Converted fee with buffer: ${convertedAmountWithBuffer.amountFromPlanks(paymentAsset.precision)}")

        return SubstrateFee(
            amount = convertedAmountWithBuffer,
            submissionOrigin = nativeFee.submissionOrigin,
            asset = paymentAsset
        )
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val accountId = metaAccount.requireAccountIdIn(chain)

        val assetConversion = hydraDxQuoteSharedComputation.getSwapQuoting(chain, accountId, coroutineScope)
        return assetConversion.canPayFeeInNonUtilityToken(paymentAsset)
    }
}
