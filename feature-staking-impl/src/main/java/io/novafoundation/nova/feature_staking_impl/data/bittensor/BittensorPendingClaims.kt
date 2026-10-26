package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

fun interface BittensorClaimClock {
    fun nowMillis(): Long
}

data class BittensorClaimTarget(val chainId: String, val coldkey: AccountIdKey, val hotkey: AccountIdKey)

data class BittensorClaimLease internal constructor(val target: BittensorClaimTarget, internal val generation: Long)

/** Process-local duplicate prevention only; not a submission journal or outcome recovery mechanism. */
class BittensorPendingClaims(private val clock: BittensorClaimClock) {

    private data class Entry(val lease: BittensorClaimLease, val acquiredAt: Long)

    private data class PendingSnapshot(val targets: Set<AccountIdKey>, val nextExpiryInMillis: Long?)

    private val entries = mutableMapOf<BittensorClaimTarget, Entry>()

    private val changes = MutableStateFlow(0L)

    private var generation = 0L

    @Synchronized
    fun tryAcquire(target: BittensorClaimTarget): BittensorClaimLease? {
        val now = clock.nowMillis()
        entries.entries.removeAll { now - it.value.acquiredAt >= PENDING_MILLIS }
        if (target in entries) return null
        val lease = BittensorClaimLease(target, ++generation)
        entries[target] = Entry(lease, now)
        changes.value += 1
        return lease
    }

    @Synchronized
    fun finish(lease: BittensorClaimLease, result: BittensorOperationResult) {
        val terminal = when (result) {
            is BittensorOperationResult.Success -> true
            is BittensorOperationResult.Failure -> result.stage != BittensorFailureStage.UNKNOWN
            is BittensorOperationResult.Unknown -> false
        }
        if (terminal && entries[lease.target]?.lease == lease) {
            entries.remove(lease.target)
            changes.value += 1
        }
    }

    @Synchronized
    fun reconcileFreshClaims(chainId: String, coldkey: AccountIdKey, claims: BittensorRootClaims, status: BittensorDataStatus) {
        if (status !is BittensorDataStatus.Fresh) return
        val eligible = claims.claimable.map { it.hotkey }.toSet()
        val readStartedAt = claims.readStartedAtMillis
        val now = clock.nowMillis()
        val changed = entries.entries.removeAll { (target, entry) ->
            now - entry.acquiredAt >= PENDING_MILLIS ||
                readStartedAt != null && entry.acquiredAt < readStartedAt &&
                target.chainId == chainId && target.coldkey == coldkey && target.hotkey !in eligible
        }
        if (changed) changes.value += 1
    }

    fun pendingTargets(chainId: String, coldkey: AccountIdKey): Set<AccountIdKey> = snapshot(chainId, coldkey).targets

    fun observe(chainId: String, coldkey: AccountIdKey) = changes.flatMapLatest {
        flow {
            while (true) {
                val snapshot = snapshot(chainId, coldkey)
                emit(snapshot.targets)
                val wait = snapshot.nextExpiryInMillis ?: break
                delay(wait)
            }
        }
    }

    @Synchronized
    private fun snapshot(chainId: String, coldkey: AccountIdKey): PendingSnapshot {
        val now = clock.nowMillis()
        val active = entries.filter { (target, entry) ->
            target.chainId == chainId && target.coldkey == coldkey && now - entry.acquiredAt < PENDING_MILLIS
        }
        return PendingSnapshot(
            targets = active.keys.map { it.hotkey }.toSet(),
            nextExpiryInMillis = active.values.minOfOrNull { PENDING_MILLIS - (now - it.acquiredAt) }
        )
    }

    companion object {
        const val PENDING_MILLIS = 15 * 60 * 1_000L
    }
}
