package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.XYKSwapQuotingSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.XYKSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSourceEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxStandaloneSwapBuilder
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import kotlinx.coroutines.flow.Flow

class XYKSwapSourceFactory: HydraDxSwapSource.Factory<XYKSwapQuotingSource> {

    override val identifier: String = XYKSwapQuotingSourceFactory.ID

    override fun create(delegate: XYKSwapQuotingSource): HydraDxSwapSource {
        return XYKSwapSource(delegate)
    }
}

private class XYKSwapSource(
    private val delegate: XYKSwapQuotingSource,
) : HydraDxSwapSource, Identifiable by delegate {

    override suspend fun sync() {
        return delegate.sync()
    }

    override suspend fun availableSwapDirections(): Collection<HydraDxSourceEdge> {
        return delegate.availableSwapDirections().map(::XYKSwapEdge)
    }

    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> {
        return delegate.runSubscriptions(userAccountId, subscriptionBuilder)
    }

    inner class XYKSwapEdge(
        private val delegate: XYKSwapQuotingSource.Edge
    ) : HydraDxSourceEdge, QuotableEdge by delegate  {

        override fun routerPoolArgument(): DictEnum.Entry<*> {
            return DictEnum.Entry("XYK", null)
        }

        override val standaloneSwapBuilder: HydraDxStandaloneSwapBuilder? = null

        override suspend fun debugLabel(): String {
            return "XYK"
        }
    }
}
