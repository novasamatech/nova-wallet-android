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
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
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
        val fromAsset = chain.commissionAsset

        val quoter = hydraDxQuoteSharedComputation.getQuoter(chain, accountId, coroutineScope)

        val convertedAmount = runCatching {
            quoter.findBestPath(
                chainAssetIn = fromAsset,
                chainAssetOut = paymentAsset,
                amount = nativeFee.amount,
                swapDirection = SwapDirection.SPECIFIED_IN
            ).bestPath.quote
        }
            .recoverCatching { hydrationPriceConversionFallback.convertNativeAmount(nativeFee.amount, paymentAsset) }
            .getOrThrow()

        return SubstrateFee(
            amount = convertedAmount,
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
