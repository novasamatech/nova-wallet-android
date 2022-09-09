package io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

class TuringAutomationTask(
    val id: String,
    val delegator: AccountId,
    val collator: AccountId,
    val accountMinimum: Balance,
    val frequencyInSeconds: BigInteger
)
