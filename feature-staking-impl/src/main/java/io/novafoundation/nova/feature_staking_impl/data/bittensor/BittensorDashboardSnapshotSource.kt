package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Last fresh dashboard figures, shown until the chain answers again (cold start, offline). */
data class BittensorDashboardSnapshot(
    val totalStakeInPlanks: BigInteger?,
    val rootStakeInPlanks: BigInteger?,
    val subnetCount: Int,
    val rootAnnualPercentageYield: BigDecimal?,
    val subnetAnnualPercentageYield: BigDecimal?,
    val maximumAnnualPercentageYield: BigDecimal?
)

/** Owns wallet-scoped dashboard snapshots and the updater's last known claimable rewards. */
class BittensorDashboardSnapshotSource(
    private val cache: ResponseCache,
    private val legacyPreferences: Preferences,
    private val gson: Gson,
    private val logger: Logger
) {

    private val mutex = Mutex()

    private data class ClaimableRewards(val amount: BigInteger?)

    private class LegacySnapshot(
        val total: String?,
        val root: String?,
        val subnets: Int?,
        val rootRate: String?,
        val subnetRate: String?,
        val maxRate: String?
    ) {
        fun toSnapshot() = BittensorDashboardSnapshot(
            totalStakeInPlanks = total?.toBigInteger(),
            rootStakeInPlanks = root?.toBigInteger(),
            subnetCount = requireNotNull(subnets),
            rootAnnualPercentageYield = rootRate?.toBigDecimal(),
            subnetAnnualPercentageYield = subnetRate?.toBigDecimal(),
            maximumAnnualPercentageYield = maxRate?.toBigDecimal()
        )
    }

    suspend fun snapshot(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): BittensorDashboardSnapshot? = mutex.withLock {
        val key = BittensorCacheKeys.dashboard(chainId, coldKey)
        cache.readSnapshot(key, BittensorDashboardSnapshot::class.java, metaAccountId, ::validateSnapshot)?.let { return@withLock it }

        val legacyKey = legacyKey("snapshot", chainId, coldKey)
        val raw = legacyPreferences.getString(legacyKey) ?: return@withLock null
        val snapshot = runCatching {
            requireNotNull(gson.fromJson(raw, LegacySnapshot::class.java)).toSnapshot().also(::validateSnapshot)
        }.getOrNull() ?: return@withLock null
        if (!persist(key, metaAccountId, snapshot)) return@withLock null
        // Remove the old copy only once Room can restore it. Failed writes must not lose the offline fallback.
        if (cache.readSnapshot(key, BittensorDashboardSnapshot::class.java, metaAccountId) != null) legacyPreferences.removeField(legacyKey)
        snapshot
    }

    suspend fun saveSnapshot(chainId: ChainId, coldKey: AccountId, metaAccountId: Long, snapshot: BittensorDashboardSnapshot) = mutex.withLock {
        validateSnapshot(snapshot)
        val key = BittensorCacheKeys.dashboard(chainId, coldKey)
        persist(key, metaAccountId, snapshot)
        val legacyKey = legacyKey("snapshot", chainId, coldKey)
        if (legacyPreferences.contains(legacyKey) && cache.readSnapshot(key, BittensorDashboardSnapshot::class.java, metaAccountId) != null) {
            legacyPreferences.removeField(legacyKey)
        }
    }

    suspend fun claimableRootRewards(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): BigInteger? = mutex.withLock {
        readClaimableRootRewards(chainId, coldKey, metaAccountId)
    }

    private suspend fun readClaimableRootRewards(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): BigInteger? {
        val key = BittensorCacheKeys.claimableRewards(chainId, coldKey)
        cache.readSnapshot(key, ClaimableRewards::class.java, metaAccountId, ::validateRewards)?.let { return it.amount }

        val legacyKey = legacyKey("claimable", chainId, coldKey)
        val amount = legacyPreferences.getString(legacyKey)?.toBigIntegerOrNull()?.takeIf { it.isPositive() } ?: return null
        if (!persist(key, metaAccountId, ClaimableRewards(amount))) return null
        if (cache.readSnapshot(key, ClaimableRewards::class.java, metaAccountId) != null) legacyPreferences.removeField(legacyKey)
        return amount
    }

    fun claimableRootRewardsFlow(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BigInteger?> = flow {
        emit(claimableRootRewards(chainId, coldKey, metaAccountId))
        emitAll(
            cache.observeSnapshot(
                key = BittensorCacheKeys.claimableRewards(chainId, coldKey),
                type = ClaimableRewards::class.java,
                metaAccountId = metaAccountId,
                validate = ::validateRewards
            ).map { stored ->
                if (stored == null) claimableRootRewards(chainId, coldKey, metaAccountId) else stored.amount
            }
        )
    }.distinctUntilChanged()

    /** Null or zero clears the value without discarding the owner's other cached data. */
    suspend fun saveClaimableRootRewards(chainId: ChainId, coldKey: AccountId, metaAccountId: Long, amount: BigInteger?) = mutex.withLock {
        val value = amount?.takeIf { it.isPositive() }
        val key = BittensorCacheKeys.claimableRewards(chainId, coldKey)
        if (readClaimableRootRewards(chainId, coldKey, metaAccountId) != value) persist(key, metaAccountId, ClaimableRewards(value))
        val legacyKey = legacyKey("claimable", chainId, coldKey)
        if (legacyPreferences.contains(legacyKey) && cache.readSnapshot(key, ClaimableRewards::class.java, metaAccountId) != null) {
            legacyPreferences.removeField(legacyKey)
        }
    }

    private suspend fun <T> persist(key: String, metaAccountId: Long, value: T): Boolean = runCatching {
        cache.saveSnapshot(key, metaAccountId, value)
    }.onFailure { error ->
        if (error is CancellationException) throw error
        logger.log("[BittensorDashboard] Snapshot could not be saved")
        logger.log(error)
    }.getOrDefault(false)

    private fun validateSnapshot(snapshot: BittensorDashboardSnapshot) {
        require(snapshot.subnetCount >= 0)
        require(snapshot.totalStakeInPlanks?.isNonNegative != false)
        require(snapshot.rootStakeInPlanks?.isNonNegative != false)
        require(snapshot.rootAnnualPercentageYield?.isNonNegative != false)
        require(snapshot.subnetAnnualPercentageYield?.isNonNegative != false)
        require(snapshot.maximumAnnualPercentageYield?.isNonNegative != false)
    }

    private fun validateRewards(rewards: ClaimableRewards) {
        require(rewards.amount == null || rewards.amount.isPositive())
    }

    private fun legacyKey(resource: String, chainId: ChainId, coldKey: AccountId): String {
        val accountKey = (chainId.toByteArray() + coldKey).blake2b256().toHexString()
        return "bittensor_dashboard_${resource}_$accountKey"
    }
}
