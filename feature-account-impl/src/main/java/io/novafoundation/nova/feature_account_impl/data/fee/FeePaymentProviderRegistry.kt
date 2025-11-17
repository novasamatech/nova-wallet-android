package io.novafoundation.nova.feature_account_impl.data.fee

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.HydrationFeePaymentProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

internal class RealFeePaymentProviderRegistry(
    private val assetHubFactory: AssetHubFeePaymentProvider.Factory,
    private val hydrationFactory: HydrationFeePaymentProvider.Factory,
    private val chainRegistry: ChainRegistry,
) : FeePaymentProviderRegistry {

    override suspend fun providerFor(chainId: ChainId): FeePaymentProvider {
        val chain = chainRegistry.getChain(chainId)

        return when (chainId) {
            Chain.Geneses.POLKADOT_ASSET_HUB -> assetHubFactory.create(chain)
            Chain.Geneses.HYDRA_DX -> hydrationFactory.create(chain)
            else -> DefaultFeePaymentProvider(chain)
        }
    }
}
