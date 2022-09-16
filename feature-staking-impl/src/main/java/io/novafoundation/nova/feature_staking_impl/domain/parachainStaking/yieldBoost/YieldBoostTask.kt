package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlin.time.Duration
import kotlin.time.DurationUnit

class YieldBoostTask(
    val id: String,
    val collator: AccountId,
    val accountMinimum: Balance,
    val frequency: Duration,
)

fun YieldBoostTask.frequencyInDays() = frequency.toInt(DurationUnit.DAYS).coerceAtLeast(1)

fun List<YieldBoostTask>.findByCollator(collatorId: AccountId): YieldBoostTask? = find { it.collator.contentEquals(collatorId) }
