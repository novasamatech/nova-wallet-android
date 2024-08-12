package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxSwapSourceId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

class OmniPoolSwapSourceFactory(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxSwapSource.Factory {

    companion object {

        const val SOURCE_ID = "OmniPool"
    }

    override fun create(chain: Chain): HydraDxSwapSource {
        return OmniPoolSwapSource(
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
        )
    }
}

private class OmniPoolSwapSource(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxSwapSource {

    override val identifier: HydraDxSwapSourceId = OmniPoolSwapSourceFactory.SOURCE_ID

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        val assetIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetIn)
        val assetIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetOut)

        when (val limit = args.swapLimit) {
            is SwapLimit.SpecifiedIn -> sell(
                assetIdIn = assetIdIn,
                assetIdOut = assetIdOut,
                amountIn = limit.expectedAmountIn,
                minBuyAmount = limit.amountOutMin
            )
            is SwapLimit.SpecifiedOut -> buy(
                assetIdIn = assetIdIn,
                assetIdOut = assetIdOut,
                amountOut = limit.expectedAmountOut,
                maxSellAmount = limit.amountInMax
            )
        }
    }

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        return DictEnum.Entry("Omnipool", null)
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
