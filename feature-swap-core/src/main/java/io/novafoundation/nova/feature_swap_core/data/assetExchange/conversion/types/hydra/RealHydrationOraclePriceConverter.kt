package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.multiTransactionPayment
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.omnipool
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationOraclePriceConverter
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named

/**
 * `FixedU128::DIV` - the scale substrate's fixed point numbers are stored at
 */
private val FIXED_U128_DIV = BigInteger.TEN.pow(18)

/**
 * One in the runtime oracle's U1F127 fixed point (127 fractional bits)
 */
private val FRACTION_ONE = BigInteger.ONE.shiftLeft(127)

/**
 * `1 - s` for the fee oracle period - the per-block decay of a stale entry's weight
 */
private val SMOOTHING_COMPLEMENT = FRACTION_ONE - HydrationOnChain.TEN_MINUTES_SMOOTHING

/**
 * `(1-s)^k` underflows the runtime's fixed point to exactly zero around k=4400 (~7.3h of staleness),
 * from where the fast-forwarded price equals the last-trade price
 */
private const val SMOOTHING_SATURATION_BLOCKS = 4400

/**
 * An exact price ratio, mirroring the runtime's `EmaPrice`. Kept as a pair of integers for the whole
 * computation since the runtime only collapses it into a fixed point number at the very last step
 */
private class PriceRatio(val numerator: BigInteger, val denominator: BigInteger) {

    companion object {

        val ONE = PriceRatio(BigInteger.ONE, BigInteger.ONE)
    }
}

private operator fun PriceRatio.times(other: PriceRatio): PriceRatio {
    return PriceRatio(numerator * other.numerator, denominator * other.denominator)
}

private class HydrationFeeContext(
    val nativeAssetId: HydraDxAssetId,
    val hubAssetId: HydraDxAssetId,
    val parentBlock: BigInteger
)

@FeatureScope
class RealHydrationOraclePriceConverter @Inject constructor(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,
) : HydrationOraclePriceConverter {

    override suspend fun convertNativeAmount(nativeAmount: BalanceOf, conversionTarget: Chain.Asset): BalanceOf? {
        if (conversionTarget.isUtilityAsset) return nativeAmount

        val chainId = conversionTarget.chainId
        val targetOnChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(conversionTarget)

        return runCatching {
            val context = loadContext(chainId)

            // `AssetFeeOraclePriceProvider` prices the target asset against the native one, then scales the native fee by it
            val route = loadRoute(chainId, from = targetOnChainId, to = context.nativeAssetId)
            val price = routePrice(chainId, route, context)

            convertFeeWithPrice(nativeAmount, price)
        }.getOrNull()
    }

    private suspend fun loadContext(chainId: String): HydrationFeeContext {
        return remoteStorageSource.query(chainId) {
            HydrationFeeContext(
                nativeAssetId = metadata.multiTransactionPayment().numberConstant("NativeAssetId", runtime),
                hubAssetId = metadata.omnipool().numberConstant("HubAssetId", runtime),
                // The chain prices fees during `on_initialize` against the parent block's oracle state;
                // the current head is the closest available stand-in for the parent of the inclusion block
                parentBlock = metadata.hydrationSystem.number.query() ?: error("No block number")
            )
        }
    }

    /**
     * Mirrors `RouteProvider::get_route`: routes are stored under the ordered asset pair and inverted when
     * requested the other way around. A pair with no registered route falls back to a single omnipool hop
     * (`DefaultRoutePoolType`)
     */
    private suspend fun loadRoute(
        chainId: String,
        from: HydraDxAssetId,
        to: HydraDxAssetId
    ): List<HydrationRouteHop> {
        val ordered = from <= to
        val key = if (ordered) hydrationAssetPairKey(from, to) else hydrationAssetPairKey(to, from)

        val stored = remoteStorageSource.query(chainId) {
            metadata.hydrationRouter.routes.query(key)
        }

        return when {
            stored == null -> listOf(HydrationRouteHop(HydrationOnChain.PoolType.OMNIPOOL, poolId = null, assetIn = from, assetOut = to))
            ordered -> stored
            else -> stored.inverted()
        }
    }

    private fun List<HydrationRouteHop>.inverted(): List<HydrationRouteHop> {
        return asReversed().map { hop ->
            HydrationRouteHop(pool = hop.pool, poolId = hop.poolId, assetIn = hop.assetOut, assetOut = hop.assetIn)
        }
    }

    /**
     * Mirrors `OraclePriceProvider::price` - the product of the per-hop prices
     */
    private suspend fun routePrice(
        chainId: String,
        route: List<HydrationRouteHop>,
        context: HydrationFeeContext
    ): PriceRatio {
        require(route.isNotEmpty()) { "Empty route" }

        return route.fold(PriceRatio.ONE) { acc, hop ->
            acc * hopPrice(chainId, hop, context)
        }
    }

    private suspend fun hopPrice(chainId: String, hop: HydrationRouteHop, context: HydrationFeeContext): PriceRatio {
        return when (hop.pool) {
            // aToken <-> underlying is always 1:1 and the runtime keeps no oracle entry for it
            HydrationOnChain.PoolType.AAVE -> PriceRatio.ONE

            HydrationOnChain.PoolType.OMNIPOOL -> composeVia(chainId, HydrationOnChain.OracleSource.OMNIPOOL, hop, context.hubAssetId, context)

            HydrationOnChain.PoolType.STABLESWAP -> {
                val shareToken = hop.poolId ?: error("Stableswap hop without a pool id")

                composeVia(chainId, HydrationOnChain.OracleSource.STABLESWAP, hop, shareToken, context)
            }

            HydrationOnChain.PoolType.XYK -> oraclePrice(chainId, HydrationOnChain.OracleSource.XYK, hop.assetIn, hop.assetOut, context)

            else -> error("Unsupported pool type in route: ${hop.pool}")
        }
    }

    /**
     * Omnipool prices everything against the hub asset and stableswap against the pool's share token,
     * so those hops are the product of the two legs through [intermediate]
     */
    private suspend fun composeVia(
        chainId: String,
        source: ByteArray,
        hop: HydrationRouteHop,
        intermediate: HydraDxAssetId,
        context: HydrationFeeContext
    ): PriceRatio {
        val toIntermediate = oraclePrice(chainId, source, hop.assetIn, intermediate, context)
        val fromIntermediate = oraclePrice(chainId, source, intermediate, hop.assetOut, context)

        return toIntermediate * fromIntermediate
    }

    /**
     * Mirrors `EmaOracle::get_price`: entries are stored against the ordered asset pair and inverted when
     * asked for in the opposite order, while an asset against itself is priced as one. The chain never uses
     * the stored value directly - a stale entry is first advanced to the current block ([fastForward]),
     * which is why the `LastBlock` entry is read alongside the fee period one
     */
    private suspend fun oraclePrice(
        chainId: String,
        source: ByteArray,
        assetA: HydraDxAssetId,
        assetB: HydraDxAssetId,
        context: HydrationFeeContext
    ): PriceRatio {
        if (assetA == assetB) return PriceRatio.ONE

        val lower = minOf(assetA, assetB)
        val higher = maxOf(assetA, assetB)

        val entriesByPeriod = remoteStorageSource.query(chainId) {
            metadata.hydrationEmaOracle.oracles.entries(
                listOf(
                    Triple(source, lower to higher, HydrationOnChain.FEE_ORACLE_PERIOD),
                    Triple(source, lower to higher, HydrationOnChain.LAST_BLOCK_ORACLE_PERIOD)
                )
            )
        }

        val stored = entriesByPeriod.entryFor(HydrationOnChain.FEE_ORACLE_PERIOD, lower, higher)
        val lastBlock = entriesByPeriod.entryFor(HydrationOnChain.LAST_BLOCK_ORACLE_PERIOD, lower, higher)

        val current = fastForward(stored, lastBlock, context.parentBlock)

        return if (assetA == lower) current else PriceRatio(current.denominator, current.numerator)
    }

    private fun Map<Triple<ByteArray, Pair<HydraDxAssetId, HydraDxAssetId>, String>, HydrationOracleEntry>.entryFor(
        period: String,
        lower: HydraDxAssetId,
        higher: HydraDxAssetId
    ): HydrationOracleEntry {
        val entry = entries.firstOrNull { it.key.third == period }?.value
            ?: error("No $period oracle entry for ($lower, $higher)")

        require(entry.numerator.signum() > 0 && entry.denominator.signum() > 0) {
            "Degenerate oracle price for ($lower, $higher): ${entry.numerator}/${entry.denominator}"
        }

        return entry
    }

    /**
     * Mirrors `EmaOracle::get_updated_entry`: an entry that was not updated in the parent block is advanced
     * to it by iterating the EMA toward the last-trade (`LastBlock`) price:
     * `new = stored * (1-s)^k + lastBlock * (1 - (1-s)^k)`, with k blocks of staleness.
     *
     * Computed with exact rationals; the runtime's per-step fixed point rounding inside the pow differs
     * from this by less than 2^-100 relatively - far below a plank at any realistic amount
     */
    private fun fastForward(
        stored: HydrationOracleEntry,
        lastBlock: HydrationOracleEntry,
        parentBlock: BigInteger
    ): PriceRatio {
        val staleBlocks = (parentBlock - stored.updatedAt).toInt()

        return when {
            staleBlocks <= 0 -> PriceRatio(stored.numerator, stored.denominator)

            staleBlocks >= SMOOTHING_SATURATION_BLOCKS -> PriceRatio(lastBlock.numerator, lastBlock.denominator)

            else -> {
                val decay = SMOOTHING_COMPLEMENT.pow(staleBlocks)
                val one = BigInteger.ONE.shiftLeft(127 * staleBlocks)

                PriceRatio(
                    numerator = stored.numerator * decay * lastBlock.denominator +
                        lastBlock.numerator * (one - decay) * stored.denominator,
                    denominator = stored.denominator * one * lastBlock.denominator
                )
            }
        }
    }

    /**
     * Mirrors `convert_fee_with_price`: `FixedU128::from_rational` (round to nearest, ties down), then
     * `checked_mul_int` (truncating), with a floor of one plank
     */
    private fun convertFeeWithPrice(nativeAmount: BalanceOf, price: PriceRatio): BalanceOf {
        val (quotient, remainder) = (price.numerator * FIXED_U128_DIV).divideAndRemainder(price.denominator)
        val fixedPointPrice = if (remainder.shiftLeft(1) > price.denominator) quotient + BigInteger.ONE else quotient

        val converted = fixedPointPrice * nativeAmount / FIXED_U128_DIV

        return converted.max(BigInteger.ONE)
    }
}
