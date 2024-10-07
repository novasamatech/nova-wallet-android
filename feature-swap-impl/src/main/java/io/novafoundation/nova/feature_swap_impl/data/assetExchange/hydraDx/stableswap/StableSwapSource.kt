package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.StableSwapQuotingSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.StableSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.network.toChainAssetOrThrow
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSourceEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxStandaloneSwapBuilder
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import kotlinx.coroutines.flow.Flow

class StableSwapSourceFactory(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxSwapSource.Factory<StableSwapQuotingSource> {

    override fun create(delegate: StableSwapQuotingSource): HydraDxSwapSource {
        return StableSwapSource(
            delegate = delegate,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter
        )
    }

    override val identifier: String = StableSwapQuotingSourceFactory.ID
}

private class StableSwapSource(
    private val delegate: StableSwapQuotingSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxSwapSource, Identifiable by delegate {

    private val chain = delegate.chain

    override suspend fun sync() {
        return delegate.sync()
    }

    override suspend fun availableSwapDirections(): Collection<HydraDxSourceEdge> {
        return delegate.availableSwapDirections().map(::StableSwapEdge)
    }

    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> {
        return delegate.runSubscriptions(userAccountId, subscriptionBuilder)
    }

    inner class StableSwapEdge(
        private val delegate: StableSwapQuotingSource.Edge
    ) : HydraDxSourceEdge, QuotableEdge by delegate {

        override val standaloneSwapBuilder: HydraDxStandaloneSwapBuilder? = null

        override suspend fun debugLabel(): String {
            val poolAsset = hydraDxAssetIdConverter.toChainAssetOrThrow(chain, delegate.poolId)
            return "StableSwap.${poolAsset.symbol}"
        }

        override fun routerPoolArgument(): DictEnum.Entry<*> {
            return DictEnum.Entry("Stableswap", delegate.poolId)
        }
    }
}
