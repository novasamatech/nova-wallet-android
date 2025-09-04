package io.novafoundation.nova.feature_account_api.data.fee

import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope

interface FeePayment : CustomFeeCapability {

    suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder)

    suspend fun convertNativeFee(nativeFee: Fee): Fee
}

interface FeePaymentProvider {

    suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment

    suspend fun detectFeePaymentFromExtrinsic(extrinsic: SendableExtrinsic): FeePayment

    suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability?>
}

interface FeePaymentProviderRegistry {

    suspend fun providerFor(chainId: ChainId): FeePaymentProvider
}
