package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.types.AssetConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.HydrationConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.NativeFeePayment
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class HydrationFeePaymentProvider(
    private val chainRegistry: ChainRegistry,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
) : FeePaymentProvider {

    override suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency): FeePayment {
        return when (feePaymentCurrency) {
            is FeePaymentCurrency.Asset -> HydrationConversionFeePayment(
                paymentAsset = feePaymentCurrency.asset,
                chainRegistry = chainRegistry,
                multiChainRuntimeCallsApi = multiChainRuntimeCallsApi
            )

            FeePaymentCurrency.Native -> NativeFeePayment()
        }
    }
}
