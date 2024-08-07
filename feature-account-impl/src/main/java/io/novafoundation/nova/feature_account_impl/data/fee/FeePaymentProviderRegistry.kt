package io.novafoundation.nova.feature_account_impl.data.fee

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class RealFeePaymentProviderRegistry(
    private val default: DefaultFeePaymentProvider,
    private val assetHub: AssetHubFeePaymentProvider
) : FeePaymentProviderRegistry {

    override suspend fun providerFor(chain: Chain): FeePaymentProvider {
        return when (chain.id) {
            Chain.Geneses.POLKADOT_ASSET_HUB -> assetHub
            Chain.Geneses.HYDRA_DX -> TODO("not implemented")
            else -> default
        }
    }
}
