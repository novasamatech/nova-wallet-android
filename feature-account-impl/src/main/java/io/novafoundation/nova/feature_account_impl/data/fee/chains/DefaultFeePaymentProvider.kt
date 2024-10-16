package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.types.NativeFeePayment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import kotlinx.coroutines.CoroutineScope

class DefaultFeePaymentProvider : FeePaymentProvider {

    override suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment {
        return NativeFeePayment()
    }

    override suspend fun fastLookupCustomFeeCapability(): FastLookupCustomFeeCapability {
        return DefaultFastLookupCustomFeeCapability()
    }
}

class DefaultFastLookupCustomFeeCapability(): FastLookupCustomFeeCapability {

    override suspend fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean {
        return false
    }
}
