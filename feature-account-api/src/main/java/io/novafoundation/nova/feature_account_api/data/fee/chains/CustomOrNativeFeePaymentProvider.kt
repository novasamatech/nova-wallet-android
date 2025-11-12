package io.novafoundation.nova.feature_account_api.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.types.NativeFeePayment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

abstract class CustomOrNativeFeePaymentProvider : FeePaymentProvider {

    protected abstract suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment

    protected abstract suspend fun canPayFeeInNonUtilityToken(customFeeAsset: Chain.Asset): Result<Boolean>

    final override suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment {
        return when (feePaymentCurrency) {
            is FeePaymentCurrency.Asset -> feePaymentFor(feePaymentCurrency.asset, coroutineScope)

            FeePaymentCurrency.Native -> NativeFeePayment()
        }
    }

    final override suspend fun canPayFee(feePaymentCurrency: FeePaymentCurrency): Result<Boolean> {
        return when (feePaymentCurrency) {
            is FeePaymentCurrency.Asset -> canPayFeeInNonUtilityToken(feePaymentCurrency.asset)
            FeePaymentCurrency.Native -> Result.success(true)
        }
    }
}
