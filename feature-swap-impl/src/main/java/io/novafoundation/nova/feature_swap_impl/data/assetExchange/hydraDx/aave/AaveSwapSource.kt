package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.aave

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.AavePoolQuotingSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.AaveSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSourceEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@FeatureScope
class AaveSwapSourceFactory @Inject constructor() : HydraDxSwapSource.Factory<AavePoolQuotingSource> {

    override val identifier: String = AaveSwapQuotingSourceFactory.ID

    override fun create(delegate: AavePoolQuotingSource): HydraDxSwapSource {
        return AaveSwapSource(delegate)
    }
}

private class AaveSwapSource(
    private val delegate: AavePoolQuotingSource,
) : HydraDxSwapSource, Identifiable by delegate {

    override suspend fun sync() {
        return delegate.sync()
    }

    override suspend fun availableSwapDirections(): Collection<HydraDxSourceEdge> {
        return delegate.availableSwapDirections().map(::AaveSwapEdge)
    }

    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> {
        return delegate.runSubscriptions(userAccountId, subscriptionBuilder)
    }

    inner class AaveSwapEdge(
        private val delegate: AavePoolQuotingSource.Edge
    ) : HydraDxSourceEdge, QuotableEdge by delegate {

        override fun routerPoolArgument(): DictEnum.Entry<*> {
            return DictEnum.Entry("Aave", null)
        }

        override val standaloneSwap = null

        override suspend fun debugLabel(): String {
            return "Aave"
        }
    }
}
