package io.novafoundation.nova.feature_account_api.data.fee

import android.util.Log
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope

interface FeePayment {

    suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder)

    suspend fun convertNativeFee(nativeFee: Fee): Fee
}

interface FeePaymentProvider {

    val chain: Chain

    suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment

    suspend fun detectFeePaymentFromExtrinsic(extrinsic: SendableExtrinsic): FeePayment

    suspend fun canPayFee(feePaymentCurrency: FeePaymentCurrency): Result<Boolean>

    suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability>
}

interface FeePaymentProviderRegistry {

    suspend fun providerFor(chainId: ChainId): FeePaymentProvider
}

suspend fun FeePaymentProvider.fastLookupCustomFeeCapabilityOrDefault(): FastLookupCustomFeeCapability {
    return fastLookupCustomFeeCapability()
        .onFailure { Log.e("FeePaymentProvider", "Failed to construct fast custom fee lookup for chain ${chain.name}", it) }
        .getOrElse { DefaultFastLookupCustomFeeCapability() }
}
