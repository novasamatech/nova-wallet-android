package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlin.time.Duration
import kotlin.time.DurationUnit

class YieldBoostTask(
    val id: String,
    val collator: AccountId,
    val accountMinimum: Balance,
    val schedule: Schedule,
) {

    sealed interface Schedule {
        object Unknown : Schedule

        class Recurring(val frequency: Duration) : Schedule
    }
}

fun YieldBoostTask.frequencyInDays() = when (schedule) {
    is YieldBoostTask.Schedule.Recurring -> schedule.frequency.toInt(DurationUnit.DAYS).coerceAtLeast(1)
    YieldBoostTask.Schedule.Unknown -> null
}

fun List<YieldBoostTask>.findByCollator(collatorId: AccountId): YieldBoostTask? = find { it.collator.contentEquals(collatorId) }
