package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.fee.types.NativeFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydrationConversionFeePayment
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope

class HydrationFeePaymentProvider(
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
    private val accountRepository: AccountRepository
) : FeePaymentProvider {

    override suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment {
        return when (feePaymentCurrency) {
            is FeePaymentCurrency.Asset -> HydrationConversionFeePayment(
                paymentAsset = feePaymentCurrency.asset,
                chainRegistry = chainRegistry,
                hydraDxAssetIdConverter = hydraDxAssetIdConverter,
                hydraDxQuoteSharedComputation = hydraDxQuoteSharedComputation,
                accountRepository = accountRepository,
                coroutineScope = coroutineScope!!
            )

            FeePaymentCurrency.Native -> NativeFeePayment()
        }
    }
}
