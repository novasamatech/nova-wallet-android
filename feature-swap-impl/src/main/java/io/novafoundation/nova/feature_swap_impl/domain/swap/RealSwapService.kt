package io.novafoundation.nova.feature_swap_impl.domain.swap

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.accumulateMaps
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requestedAccountPaysFees
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

private const val ALL_DIRECTIONS_CACHE = "RealSwapService.ALL_DIRECTIONS"
private const val EXCHANGES_CACHE = "RealSwapService.EXCHANGES"

internal class RealSwapService(
    private val assetConversionFactory: AssetConversionExchangeFactory,
    private val hydraDxOmnipoolFactory: HydraDxExchangeFactory,
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : SwapService {

    override suspend fun canPayFeeInNonUtilityAsset(asset: Chain.Asset): Boolean = withContext(Dispatchers.Default) {
        val computationScope = CoroutineScope(coroutineContext)

        val exchange = exchanges(computationScope).getValue(asset.chainId)
        val isCustomFeeToken = !asset.isCommissionAsset
        val currentMetaAccount = accountRepository.getSelectedMetaAccount()

        // TODO we disable custom fee tokens payment for account types where current account is not the one who pays fees (e.g. it is proxied).
        // This restriction can be removed once we consider all corner-cases
        isCustomFeeToken && exchange.canPayFeeInNonUtilityToken(asset) && currentMetaAccount.type.requestedAccountPaysFees()
    }

    override suspend fun assetsAvailableForSwap(
        computationScope: CoroutineScope
    ): Flow<Set<FullChainAssetId>> {
        return allAvailableDirections(computationScope).map { it.keys }
    }

    override suspend fun availableSwapDirectionsFor(
        asset: Chain.Asset,
        computationScope: CoroutineScope
    ): Flow<Set<FullChainAssetId>> {
        return allAvailableDirections(computationScope).map { it[asset.fullId].orEmpty() }
    }

    override suspend fun quote(args: SwapQuoteArgs): Result<SwapQuote> {
        return withContext(Dispatchers.Default) {
            runCatching {
                val exchange = exchanges(this).getValue(args.tokenIn.configuration.chainId)
                val quoteArgs = AssetExchangeQuoteArgs(
                    chainAssetIn = args.tokenIn.configuration,
                    chainAssetOut = args.tokenOut.configuration,
                    amount = args.amount,
                    swapDirection = args.swapDirection
                )
                val quote = exchange.quote(quoteArgs)

                val (amountIn, amountOut) = args.inAndOutAmounts(quote)

                SwapQuote(
                    amountIn = args.tokenIn.configuration.withAmount(amountIn),
                    amountOut = args.tokenOut.configuration.withAmount(amountOut),
                    direction = args.swapDirection,
                    priceImpact = args.calculatePriceImpact(amountIn, amountOut),
                    path = quote.path
                )
            }
        }
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): SwapFee {
        val computationScope = CoroutineScope(coroutineContext)
        val exchange = exchanges(computationScope).getValue(args.assetIn.chainId)

        val assetExchangeFee = exchange.estimateFee(args)

        return SwapFee(networkFee = assetExchangeFee.networkFee, minimumBalanceBuyIn = assetExchangeFee.minimumBalanceBuyIn)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        val computationScope = CoroutineScope(coroutineContext)

        return runCatching { exchanges(computationScope).getValue(args.assetIn.chainId) }
            .flatMap { exchange -> exchange.swap(args) }
    }

    override suspend fun slippageConfig(chainId: ChainId): SlippageConfig? {
        val computationScope = CoroutineScope(coroutineContext)
        val exchanges = exchanges(computationScope)
        return exchanges[chainId]?.slippageConfig()
    }

    override fun runSubscriptions(chainIn: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            val exchanges = exchanges(scope)
            exchanges.getValue(chainIn.id).runSubscriptions(chainIn, metaAccount)
        }.throttleLast(500.milliseconds)
    }

    private fun SwapQuoteArgs.calculatePriceImpact(amountIn: Balance, amountOut: Balance): Percent {
        val fiatIn = tokenIn.planksToFiat(amountIn)
        val fiatOut = tokenOut.planksToFiat(amountOut)

        return calculatePriceImpact(fiatIn, fiatOut)
    }

    private fun SwapQuoteArgs.inAndOutAmounts(quote: AssetExchangeQuote): Pair<Balance, Balance> {
        return when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> amount to quote.quote
            SwapDirection.SPECIFIED_OUT -> quote.quote to amount
        }
    }

    private fun calculatePriceImpact(fiatIn: BigDecimal, fiatOut: BigDecimal): Percent {
        if (fiatIn.isZero || fiatOut.isZero) return Percent.zero()

        val priceImpact = (BigDecimal.ONE - fiatOut / fiatIn).atLeastZero()

        return priceImpact.asPerbill().toPercent()
    }

    private suspend fun allAvailableDirections(computationScope: CoroutineScope): Flow<MultiMap<FullChainAssetId, FullChainAssetId>> {
        return computationalCache.useSharedFlow(ALL_DIRECTIONS_CACHE, computationScope) {
            val exchanges = exchanges(computationScope)

            val directionsByExchange = exchanges.map { (chainId, exchange) ->
                flowOf { exchange.availableSwapDirections() }
                    .catch {
                        emit(emptyMap())

                        Log.e("RealSwapService", "Failed to fetch directions for exchange ${exchange::class} in chain $chainId", it)
                    }
            }

            directionsByExchange
                .accumulateMaps()
                .filter { it.isNotEmpty() }
        }
    }

    private suspend fun exchanges(computationScope: CoroutineScope): Map<ChainId, AssetExchange> {
        return computationalCache.useCache(EXCHANGES_CACHE, computationScope) {
            createExchanges(computationScope)
        }
    }

    private suspend fun createExchanges(coroutineScope: CoroutineScope): Map<ChainId, AssetExchange> {
        return chainRegistry.chainsById.first().mapValues { (_, chain) ->
            createExchange(coroutineScope, chain)
        }
            .filterNotNull()
    }

    private suspend fun createExchange(computationScope: CoroutineScope, chain: Chain): AssetExchange? {
        val factory = when {
            chain.swap.assetConversionSupported() -> assetConversionFactory
            chain.swap.hydraDxSupported() -> hydraDxOmnipoolFactory
            else -> null
        }

        return factory?.create(chain, computationScope)
    }
}
