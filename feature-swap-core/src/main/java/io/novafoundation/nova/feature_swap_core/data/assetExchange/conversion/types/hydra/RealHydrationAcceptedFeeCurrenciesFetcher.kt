package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationAcceptedFeeCurrenciesFetcher
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

internal class RealHydrationAcceptedFeeCurrenciesFetcher @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE) private val remoteStorage: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter
) : HydrationAcceptedFeeCurrenciesFetcher {

    override suspend fun fetchAcceptedFeeCurrencies(chain: Chain): Result<Set<ChainAssetId>> {
        return runCatching {
            val acceptedOnChainIds = remoteStorage.query(chain.id) {
                metadata.multiTransactionPayment.acceptedCurrencies.keys()
            }

            val onChainToLocalIds = hydraDxAssetIdConverter.allOnChainIds(chain)

            acceptedOnChainIds.mapNotNullToSet { onChainToLocalIds[it]?.id }
        }
    }
}
