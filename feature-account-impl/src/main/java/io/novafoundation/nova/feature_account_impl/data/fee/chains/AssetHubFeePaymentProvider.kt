package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.types.AssetConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.NativeFeePayment
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.CoroutineScope

class AssetHubFeePaymentProvider(
    private val chainRegistry: ChainRegistry,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val remoteStorageSource: StorageDataSource,
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
) : FeePaymentProvider {

    override suspend fun feePaymentFor(feePaymentCurrency: FeePaymentCurrency, coroutineScope: CoroutineScope?): FeePayment {
        return when (feePaymentCurrency) {
            is FeePaymentCurrency.Asset -> {
                val chain = chainRegistry.getChain(feePaymentCurrency.asset.chainId)
                AssetConversionFeePayment(
                    paymentAsset = feePaymentCurrency.asset,
                    multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
                    remoteStorageSource = remoteStorageSource,
                    multiLocationConverter = multiLocationConverterFactory.defaultAsync(chain, coroutineScope!!)
                )
            }

            FeePaymentCurrency.Native -> NativeFeePayment()
        }
    }
}
