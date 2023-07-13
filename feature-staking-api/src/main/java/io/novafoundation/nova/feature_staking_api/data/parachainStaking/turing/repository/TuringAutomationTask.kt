package io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class TuringAutomationTask(
    val id: String,
    val delegator: AccountId,
    val collator: AccountId,
    val accountMinimum: Balance,
    val schedule: Schedule
) {

    sealed interface Schedule {
        object Unknown : Schedule

        class Recurring(val frequency: BigInteger) : Schedule
    }
}
