package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.OmniPoolQuotingSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.OmniPoolQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSourceEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.StandaloneHydraSwap
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEventOrThrow
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow

private const val AMOUNT_OUT_POSITION = 4

class OmniPoolSwapSourceFactory : HydraDxSwapSource.Factory<OmniPoolQuotingSource> {

    override val identifier: String = OmniPoolQuotingSourceFactory.SOURCE_ID

    override fun create(delegate: OmniPoolQuotingSource): HydraDxSwapSource {
        return OmniPoolSwapSource(delegate)
    }
}

private class OmniPoolSwapSource(
    private val delegate: OmniPoolQuotingSource,
) : HydraDxSwapSource, Identifiable by delegate {

    override suspend fun sync() {
        return delegate.sync()
    }

    override suspend fun availableSwapDirections(): Collection<HydraDxSourceEdge> {
        return delegate.availableSwapDirections().map(::OmniPoolSwapEdge)
    }

    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> {
        return delegate.runSubscriptions(userAccountId, subscriptionBuilder)
    }

    private inner class OmniPoolSwapEdge(
        private val delegate: OmniPoolQuotingSource.Edge
    ) : HydraDxSourceEdge, QuotableEdge by delegate, StandaloneHydraSwap {

        override fun routerPoolArgument(): DictEnum.Entry<*> {
            return DictEnum.Entry("Omnipool", null)
        }

        override val standaloneSwap = this

        override suspend fun debugLabel(): String {
            return "OmniPool"
        }

        context(ExtrinsicBuilder)
        override fun addSwapCall(args: AtomicSwapOperationArgs) {
            val assetIdIn = delegate.fromAsset.first
            val assetIdOut = delegate.toAsset.first

            when (val limit = args.estimatedSwapLimit) {
                is SwapLimit.SpecifiedIn -> sell(
                    assetIdIn = assetIdIn,
                    assetIdOut = assetIdOut,
                    amountIn = limit.amountIn,
                    minBuyAmount = limit.amountOutMin
                )

                is SwapLimit.SpecifiedOut -> buy(
                    assetIdIn = assetIdIn,
                    assetIdOut = assetIdOut,
                    amountOut = limit.amountOut,
                    maxSellAmount = limit.amountInMax
                )
            }
        }

        override fun extractReceivedAmount(events: List<GenericEvent.Instance>): Balance {
            val swapExecutedEvent = events.findEvent(Modules.OMNIPOOL, "BuyExecuted")
                ?: events.findEventOrThrow(Modules.OMNIPOOL, "SellExecuted")

            val amountOut = swapExecutedEvent.arguments[AMOUNT_OUT_POSITION]
            return bindNumber(amountOut)
        }

        private fun ExtrinsicBuilder.sell(
            assetIdIn: HydraDxAssetId,
            assetIdOut: HydraDxAssetId,
            amountIn: Balance,
            minBuyAmount: Balance
        ) {
            call(
                moduleName = Modules.OMNIPOOL,
                callName = "sell",
                arguments = mapOf(
                    "asset_in" to assetIdIn,
                    "asset_out" to assetIdOut,
                    "amount" to amountIn,
                    "min_buy_amount" to minBuyAmount
                )
            )
        }

        private fun ExtrinsicBuilder.buy(
            assetIdIn: HydraDxAssetId,
            assetIdOut: HydraDxAssetId,
            amountOut: Balance,
            maxSellAmount: Balance
        ) {
            call(
                moduleName = Modules.OMNIPOOL,
                callName = "buy",
                arguments = mapOf(
                    "asset_out" to assetIdOut,
                    "asset_in" to assetIdIn,
                    "amount" to amountOut,
                    "max_sell_amount" to maxSellAmount
                )
            )
        }
    }
}

