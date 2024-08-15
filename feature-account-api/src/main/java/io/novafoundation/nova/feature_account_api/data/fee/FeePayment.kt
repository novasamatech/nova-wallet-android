package io.novafoundation.nova.feature_account_api.data.fee

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope

interface FeePayment {

    suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder)

    suspend fun convertNativeFee(nativeFee: Fee): Fee

    suspend fun availableCustomFeeAssets(): List<Chain.Asset>
}

interface FeePaymentProvider {

    suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment
}

interface FeePaymentProviderRegistry {

    suspend fun providerFor(chain: Chain): FeePaymentProvider
}