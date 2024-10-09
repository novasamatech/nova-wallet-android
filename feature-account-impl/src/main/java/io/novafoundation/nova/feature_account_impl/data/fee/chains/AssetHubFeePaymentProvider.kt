package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.types.AssetConversionFeePayment
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.CoroutineScope

class AssetHubFeePaymentProvider(
    private val chainRegistry: ChainRegistry,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val remoteStorageSource: StorageDataSource,
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
) : CustomOrNativeFeePaymentProvider() {

    override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
        val chain = chainRegistry.getChain(customFeeAsset.chainId)
        return AssetConversionFeePayment(
            paymentAsset = customFeeAsset,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            remoteStorageSource = remoteStorageSource,
            multiLocationConverter = multiLocationConverterFactory.defaultAsync(chain, coroutineScope!!)
        )
    }
}
