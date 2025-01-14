package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.network.isSystemAsset
import io.novafoundation.nova.feature_swap_core_api.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

class RealHydraDxQuotingFactory(
    private val remoteStorageSource: StorageDataSource,
    private val conversionSourceFactories: Iterable<HydraDxQuotingSource.Factory<*>>,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxQuoting.Factory {

    override fun create(chain: Chain): HydraDxQuoting {
        return RealHydraDxQuoting(
            chain = chain,
            remoteStorageSource = remoteStorageSource,
            quotingSourceFactories = conversionSourceFactories,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter
        )
    }
}

private class RealHydraDxQuoting(
    private val chain: Chain,
    private val remoteStorageSource: StorageDataSource,
    private val quotingSourceFactories: Iterable<HydraDxQuotingSource.Factory<*>>,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxQuoting {

    private val quotingSources: Map<String, HydraDxQuotingSource<*>> = createSources()

    override fun getSource(id: String): HydraDxQuotingSource<*> {
        return quotingSources.getValue(id)
    }

    override suspend fun sync() {
        quotingSources.values.forEachAsync { it.sync() }
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
        val onChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(chainAsset)

        if (hydraDxAssetIdConverter.isSystemAsset(onChainId)) return true

        val fallbackPrice = remoteStorageSource.query(chain.id) {
            metadata.multiTransactionPayment.acceptedCurrencies.query(onChainId)
        }

        return fallbackPrice != null
    }

    override suspend fun availableSwapDirections(): List<QuotableEdge> {
        return quotingSources.values.flatMapAsync { source -> source.availableSwapDirections() }
    }

    override suspend fun runSubscriptions(userAccountId: AccountId, subscriptionBuilder: SharedRequestsBuilder): Flow<Unit> {
        return quotingSources.values.map {
            it.runSubscriptions(userAccountId, subscriptionBuilder)
        }.mergeIfMultiple()
    }

    private fun createSources(): Map<String, HydraDxQuotingSource<*>> {
        return quotingSourceFactories.map { it.create(chain) }
            .associateBy { it.identifier }
    }
}
