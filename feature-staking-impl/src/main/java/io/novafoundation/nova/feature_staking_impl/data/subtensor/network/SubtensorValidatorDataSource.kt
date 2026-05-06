package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorValidator
import java.math.BigInteger

/**
 * Source of numeric validator data — total stake, commission, APR,
 * nominator count. Identity comes from the `BittensorDelegatesClient`
 * registry; this interface intentionally returns a list rather than a map
 * because some sources (TaoStats) iterate over their own paged response.
 *
 * Two implementations:
 *  - `TaoStatsValidatorDataSource` (gated to debug builds, with API key).
 *  - `StubSubtensorValidatorDataSource` (release fallback — empty list).
 *
 * Mirrors `SubtensorValidatorDataSourceProtocol` on iOS. The DI binding is
 * the swap point when Nova's own Bittensor indexer ships.
 */
interface SubtensorValidatorDataSource {

    suspend fun fetchValidators(netuid: Int): List<SubtensorValidator>
}

/** Used in release builds where no TaoStats API key is bundled. */
class StubSubtensorValidatorDataSource : SubtensorValidatorDataSource {

    override suspend fun fetchValidators(netuid: Int): List<SubtensorValidator> = emptyList()
}

/** Aggregated validator numerics from any source — keyed by SS58 hotkey. */
data class TaoStatsValidatorRow(
    val hotkeySs58: String,
    val totalStake: BigInteger,
    val ownStake: BigInteger,
    val nominatorCount: Int?,
    val commission: Double?,
    val apr: Double?,
)
