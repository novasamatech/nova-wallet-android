package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorColdkey
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Last fresh dashboard figures, shown until the chain answers again (cold start, offline). */
data class BittensorDashboardSnapshot(
    val totalStakeInPlanks: BigInteger?,
    val rootStakeInPlanks: BigInteger?,
    val subnetCount: Int,
    val rootAnnualPercentageYield: BigDecimal?,
    val subnetAnnualPercentageYield: BigDecimal?,
    val maximumAnnualPercentageYield: BigDecimal?
)

/**
 * Keyed by chain and cold key rather than wallet id, so a re-created wallet never inherits another
 * account's figures. Claimable root rewards are written by the dashboard updater, the snapshot by
 * the dashboard details source: separate keys keep a single writer per value.
 */
interface BittensorDashboardSnapshotRepository {

    fun snapshot(chainId: ChainId, coldKey: BittensorColdkey): BittensorDashboardSnapshot?

    fun saveSnapshot(chainId: ChainId, coldKey: BittensorColdkey, snapshot: BittensorDashboardSnapshot)

    fun claimableRootRewards(chainId: ChainId, coldKey: BittensorColdkey): BigInteger?

    fun claimableRootRewardsFlow(chainId: ChainId, coldKey: BittensorColdkey): Flow<BigInteger?>

    /** Null or zero clears the value. */
    fun saveClaimableRootRewards(chainId: ChainId, coldKey: BittensorColdkey, amount: BigInteger?)
}

class RealBittensorDashboardSnapshotRepository(
    private val preferences: Preferences,
    private val gson: Gson
) : BittensorDashboardSnapshotRepository {

    private class StoredSnapshot(
        val total: String?,
        val root: String?,
        val subnets: Int?,
        val rootRate: String?,
        val subnetRate: String?,
        val maxRate: String?
    )

    override fun snapshot(chainId: ChainId, coldKey: BittensorColdkey): BittensorDashboardSnapshot? {
        val raw = preferences.getString(snapshotKey(chainId, coldKey)) ?: return null
        return runCatching {
            val stored = gson.fromJson(raw, StoredSnapshot::class.java)
            BittensorDashboardSnapshot(
                totalStakeInPlanks = stored.total?.toBigInteger(),
                rootStakeInPlanks = stored.root?.toBigInteger(),
                subnetCount = requireNotNull(stored.subnets),
                rootAnnualPercentageYield = stored.rootRate?.toBigDecimal(),
                subnetAnnualPercentageYield = stored.subnetRate?.toBigDecimal(),
                maximumAnnualPercentageYield = stored.maxRate?.toBigDecimal()
            )
        }.getOrNull()
    }

    override fun saveSnapshot(chainId: ChainId, coldKey: BittensorColdkey, snapshot: BittensorDashboardSnapshot) {
        val stored = StoredSnapshot(
            total = snapshot.totalStakeInPlanks?.toString(),
            root = snapshot.rootStakeInPlanks?.toString(),
            subnets = snapshot.subnetCount,
            rootRate = snapshot.rootAnnualPercentageYield?.toPlainString(),
            subnetRate = snapshot.subnetAnnualPercentageYield?.toPlainString(),
            maxRate = snapshot.maximumAnnualPercentageYield?.toPlainString()
        )
        preferences.putString(snapshotKey(chainId, coldKey), gson.toJson(stored))
    }

    override fun claimableRootRewards(chainId: ChainId, coldKey: BittensorColdkey): BigInteger? {
        return parsePositiveAmount(preferences.getString(claimableKey(chainId, coldKey)))
    }

    override fun claimableRootRewardsFlow(chainId: ChainId, coldKey: BittensorColdkey): Flow<BigInteger?> {
        val key = claimableKey(chainId, coldKey)
        return preferences.keyFlow(key)
            .map { parsePositiveAmount(preferences.getString(key)) }
            .distinctUntilChanged()
    }

    override fun saveClaimableRootRewards(chainId: ChainId, coldKey: BittensorColdkey, amount: BigInteger?) {
        val key = claimableKey(chainId, coldKey)
        val value = amount?.takeIf { it.isPositive() }?.toString()
        if (preferences.getString(key) == value) return
        if (value == null) preferences.removeField(key) else preferences.putString(key, value)
    }

    private fun parsePositiveAmount(raw: String?): BigInteger? = raw?.toBigIntegerOrNull()?.takeIf { it.isPositive() }

    private fun snapshotKey(chainId: ChainId, coldKey: BittensorColdkey) = "bittensor_dashboard_snapshot_${accountKey(chainId, coldKey)}"

    private fun claimableKey(chainId: ChainId, coldKey: BittensorColdkey) = "bittensor_dashboard_claimable_${accountKey(chainId, coldKey)}"

    // Preferences are plain text on disk; do not store the account identifier itself.
    private fun accountKey(chainId: ChainId, coldKey: BittensorColdkey) = (chainId.toByteArray() + coldKey).blake2b256().toHexString()
}
