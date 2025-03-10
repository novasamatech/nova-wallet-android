package io.novafoundation.nova.feature_account_impl.data.fee

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProviderFactory
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.HydrationFeePaymentProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

internal class RealFeePaymentProviderRegistry(
    private val default: DefaultFeePaymentProvider,
    private val assetHubFactory: AssetHubFeePaymentProviderFactory,
    private val hydration: HydrationFeePaymentProvider
) : FeePaymentProviderRegistry {

    override suspend fun providerFor(chainId: ChainId): FeePaymentProvider {
        return when (chainId) {
            Chain.Geneses.POLKADOT_ASSET_HUB -> assetHubFactory.create(chainId)
            Chain.Geneses.HYDRA_DX -> hydration
            else -> default
        }
    }
}
