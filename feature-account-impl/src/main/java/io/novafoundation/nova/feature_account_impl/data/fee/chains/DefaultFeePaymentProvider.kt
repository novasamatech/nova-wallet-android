package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.DefaultFastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.types.NativeFeePayment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope

class DefaultFeePaymentProvider(override val chain: Chain) : FeePaymentProvider {

    override suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment {
        return NativeFeePayment()
    }

    override suspend fun detectFeePaymentFromExtrinsic(extrinsic: SendableExtrinsic): FeePayment {
        return NativeFeePayment()
    }

    override suspend fun canPayFee(feePaymentCurrency: FeePaymentCurrency): Result<Boolean> {
        val result = when (feePaymentCurrency) {
            is FeePaymentCurrency.Asset -> false
            FeePaymentCurrency.Native -> true
        }

        return Result.success(result)
    }

    override suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability> {
        return Result.success(DefaultFastLookupCustomFeeCapability())
    }
}
