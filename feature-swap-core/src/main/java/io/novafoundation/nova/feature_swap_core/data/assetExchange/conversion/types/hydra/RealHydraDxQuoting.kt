package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

class RealHydraDxQuotingFactory(
    private val conversionSourceFactories: Iterable<HydraDxQuotingSource.Factory<*>>,
) : HydraDxQuoting.Factory {

    override fun create(chain: Chain, host: SwapQuoting.QuotingHost): HydraDxQuoting {
        return RealHydraDxQuoting(
            chain = chain,
            quotingSourceFactories = conversionSourceFactories,
            host = host
        )
    }
}

private class RealHydraDxQuoting(
    private val chain: Chain,
    private val quotingSourceFactories: Iterable<HydraDxQuotingSource.Factory<*>>,
    private val host: SwapQuoting.QuotingHost,
) : HydraDxQuoting {

    private val quotingSources: Map<String, HydraDxQuotingSource<*>> = createSources()

    override fun getSource(id: String): HydraDxQuotingSource<*> {
        return quotingSources.getValue(id)
    }

    override suspend fun sync() {
        quotingSources.values.forEachAsync { it.sync() }
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
        return quotingSourceFactories.map { it.create(chain, host) }
            .associateBy { it.identifier }
    }
}
