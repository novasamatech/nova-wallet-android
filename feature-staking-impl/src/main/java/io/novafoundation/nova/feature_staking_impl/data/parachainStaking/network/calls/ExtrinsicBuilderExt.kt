package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

fun ExtrinsicBuilder.delegate(
    candidate: AccountId,
    amount: Balance,
    candidateDelegationCount: BigInteger,
    delegationCount: BigInteger
) : ExtrinsicBuilder {
    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = "delegate",
        arguments = mapOf(
            "candidate" to candidate,
            "amount" to amount,
            "candidate_delegation_count" to candidateDelegationCount,
            "delegation_count" to delegationCount
        )
    )
}
