package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import java.math.BigInteger

/**
 * Identifiers defined by the Hydration runtime itself, not by Nova.
 *
 * They are kept together to make it obvious that these strings must match the chain exactly - they are not
 * ours to rename, and they intentionally differ from the swap sources' own ids
 * (e.g. the runtime spells the pool `Stableswap`, while [RealStableSwapQuotingSource] uses `StableSwap`).
 *
 * Every value below is linked to its definition in `galacticcouncil/hydration-node`, pinned at the commit
 * these were read from. Follow the link before changing anything here.
 */
object HydrationOnChain {

    /**
     * Pool variants of the entries stored in `Router.Routes`, spelled as the `PoolType` enum declares them.
     *
     * `LBP` and `HSM` are deliberately absent: `OraclePriceProvider::price` bails out on those, and so do we.
     *
     * https://github.com/galacticcouncil/hydration-node/blob/846b2232e54045ad1a6bc02f701455b8d7835e99/traits/src/router.rs#L82-L90
     */
    object PoolType {

        const val OMNIPOOL = "Omnipool"

        const val XYK = "XYK"

        const val STABLESWAP = "Stableswap"

        const val AAVE = "Aave"
    }

    /**
     * `EmaOracle` source ids. The runtime type is `[u8; 8]`, which is why the names are truncated to 8 bytes.
     *
     * https://github.com/galacticcouncil/hydration-node/blob/846b2232e54045ad1a6bc02f701455b8d7835e99/primitives/src/constants.rs#L82-L85
     */
    object OracleSource {

        val OMNIPOOL = "omnipool".toByteArray()

        val XYK = "hydraxyk".toByteArray()

        val STABLESWAP = "stablesw".toByteArray()
    }

    /**
     * The `OraclePeriod` `MultiTransactionPayment` uses when it prices a fee in a non-native currency.
     *
     * Period enum: https://github.com/galacticcouncil/hydration-node/blob/846b2232e54045ad1a6bc02f701455b8d7835e99/traits/src/oracle.rs#L61-L74
     * Chosen at: https://github.com/galacticcouncil/hydration-node/blob/846b2232e54045ad1a6bc02f701455b8d7835e99/pallets/transaction-multi-payment/src/lib.rs#L687-L695
     */
    const val FEE_ORACLE_PERIOD = "TenMinutes"

    /**
     * The unaggregated last-trade oracle entry. When a period entry is stale, the runtime advances it
     * toward this one before use instead of returning the stored value.
     *
     * https://github.com/galacticcouncil/hydration-node/blob/846b2232e54045ad1a6bc02f701455b8d7835e99/pallets/ema-oracle/src/lib.rs#L779-L799
     */
    const val LAST_BLOCK_ORACLE_PERIOD = "LastBlock"

    /**
     * `into_smoothing(TenMinutes)` - the per-block EMA smoothing factor for [FEE_ORACLE_PERIOD], copied
     * verbatim from the runtime: `Fraction` bits in U1F127 fixed point (127 fractional bits), ~2/101 for
     * the 100-block period.
     *
     * https://github.com/galacticcouncil/hydration-node/blob/846b2232e54045ad1a6bc02f701455b8d7835e99/pallets/ema-oracle/src/types.rs#L233-L244
     */
    val TEN_MINUTES_SMOOTHING = BigInteger("3369132345751865974884897103284833777")
}
