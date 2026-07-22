package io.novafoundation.nova.feature_staking_impl.domain.subtensor.model

import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

/**
 * Static configuration shared across the Bittensor (TAO) staking surface.
 *
 * Mirrors `SubtensorStakingConstants.swift` on the iOS side. Values here are
 * verified against finney mainnet and are used by the multistaking dashboard
 * sync, the detail screen, the stake setup flow, and the extrinsic builder.
 */
object SubtensorStakingConstants {

    /** Root subnet id. Subnet alpha assets carry netuid 1..128 in `typeExtras`. */
    const val ROOT_NETUID: Int = 0

    /** Subnet alpha asset typeExtras key — value is an Int netuid. */
    const val NETUID_TYPE_EXTRA_KEY: String = "netuid"

    /**
     * Hardcoded minimum nominator stake in RAO (= 0.01 TAO). Verified against
     * finney mainnet 2026-04-13. The runtime constant exists but isn't wired
     * yet — keep this in sync if the chain bumps it.
     */
    val MIN_NOMINATOR_STAKE_RAO: BigInteger = BigInteger.valueOf(10_000_000L)

    /** Default slippage cushion for the *Limit extrinsic variants. */
    const val DEFAULT_SLIPPAGE: Double = 0.005

    /** GitHub registry for validator identity metadata. */
    const val DELEGATES_REGISTRY_URL: String =
        "https://raw.githubusercontent.com/opentensor/bittensor-delegates/main/public/delegates.json"

    /** Mainnet RPC fallback if the chain config has no usable nodes. */
    const val FALLBACK_RPC_URL: String = "https://entrypoint-finney.opentensor.ai"

    /** Multistaking dashboard re-poll interval. Same as iOS. */
    const val DASHBOARD_RESYNC_SECONDS: Long = 30

    /** Per-coldkey fetch cache TTL. */
    const val POSITION_CACHE_TTL_SECONDS: Long = 15

    /**
     * Nova Wallet service fee on subnet staking, expressed as a numerator over
     * [NOVA_FEE_DENOMINATOR]. 30 / 10_000 = 0.3%. Mirrors the shipped iOS
     * feature. Root staking (netuid == [ROOT_NETUID]) is exempt.
     */
    const val NOVA_FEE_NUMERATOR: Int = 30
    const val NOVA_FEE_DENOMINATOR: Int = 10_000

    /** Human-readable fee percentage for UI copy. */
    const val NOVA_FEE_PERCENT_DISPLAY: String = "0.3"

    /**
     * Recipient of the Nova service fee.
     *
     * `null` means the feature is fully INERT: no fee leg is added and the
     * stake/unstake extrinsics behave exactly as before. A real value MUST be
     * a validated 32-byte AccountId (the decoded public key, not an SS58
     * string) on the Bittensor chain — set it deliberately, never from
     * unvalidated input.
     */
    val NOVA_FEE_RECIPIENT: AccountId? = null

    /**
     * The Nova service fee, in planks, charged on [gross]. Integer (plank) math
     * with floor rounding: `floor(gross * 30 / 10_000)`. Sub-threshold dust
     * floors to 0.
     */
    fun novaFeeAmount(gross: BigInteger): BigInteger =
        gross * NOVA_FEE_NUMERATOR.toBigInteger() / NOVA_FEE_DENOMINATOR.toBigInteger()
}
