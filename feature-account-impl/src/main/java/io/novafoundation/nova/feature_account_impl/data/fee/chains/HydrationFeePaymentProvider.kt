package io.novafoundation.nova.feature_account_impl.data.fee.chains

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.types.NativeFeePayment
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydrationConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydrationFastLookupFeeCapability
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationAcceptedFeeCurrenciesFetcher
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope

class HydrationFeePaymentProvider @AssistedInject constructor(
    @Assisted private val chain: Chain,
    private val chainRegistry: ChainRegistry,
    private val hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
    private val hydrationFeeInjector: HydrationFeeInjector,
    private val hydrationPriceConversionFallback: HydrationPriceConversionFallback,
    private val accountRepository: AccountRepository,
    private val hydrationAcceptedFeeCurrenciesFetcher: HydrationAcceptedFeeCurrenciesFetcher
) : CustomOrNativeFeePaymentProvider() {

    @AssistedFactory
    interface Factory {

        fun create(chain: Chain): HydrationFeePaymentProvider
    }

    override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
        return HydrationConversionFeePayment(
            paymentAsset = customFeeAsset,
            chainRegistry = chainRegistry,
            hydrationFeeInjector = hydrationFeeInjector,
            hydraDxQuoteSharedComputation = hydraDxQuoteSharedComputation,
            accountRepository = accountRepository,
            coroutineScope = coroutineScope!!,
            hydrationPriceConversionFallback = hydrationPriceConversionFallback
        )
    }

    override suspend fun detectFeePaymentFromExtrinsic(extrinsic: SendableExtrinsic): FeePayment {
        // Todo Hydration fee support from extrinsic
        return NativeFeePayment()
    }

    override suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability?> {
        return hydrationAcceptedFeeCurrenciesFetcher.fetchAcceptedFeeCurrencies(chain)
            .map(::HydrationFastLookupFeeCapability)
    }
}
