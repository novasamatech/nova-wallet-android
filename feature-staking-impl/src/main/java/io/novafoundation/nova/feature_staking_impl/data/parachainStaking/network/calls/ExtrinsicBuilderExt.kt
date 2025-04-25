package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.hasCall
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import java.math.BigInteger

fun ExtrinsicBuilder.delegate(
    candidate: AccountId,
    amount: Balance,
    candidateDelegationCount: BigInteger,
    delegationCount: BigInteger
): ExtrinsicBuilder {
    return if (runtime.metadata.hasDelegateAutoCompoundCall()) {
        call(
            moduleName = Modules.PARACHAIN_STAKING,
            callName = "delegate_with_auto_compound",
            arguments = mapOf(
                "candidate" to candidate,
                "amount" to amount,
                "auto_compound" to BigInteger.ZERO,
                "candidate_delegation_count" to candidateDelegationCount,
                "candidate_auto_compounding_delegation_count" to BigInteger.ZERO,
                "delegation_count" to delegationCount
            )
        )
    } else {
        call(
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
}

private fun RuntimeMetadata.hasDelegateAutoCompoundCall(): Boolean {
    return parachainStaking().hasCall("delegate_with_auto_compound")
}

fun ExtrinsicBuilder.delegatorBondMore(
    candidate: AccountId,
    amount: Balance,
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = "delegator_bond_more",
        arguments = mapOf(
            "candidate" to candidate,
            "more" to amount
        )
    )
}

fun ExtrinsicBuilder.scheduleRevokeDelegation(
    collatorId: AccountId
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = "schedule_revoke_delegation",
        arguments = mapOf(
            "collator" to collatorId,
        )
    )
}

fun ExtrinsicBuilder.scheduleBondLess(
    collatorId: AccountId,
    amount: Balance,
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = "schedule_delegator_bond_less",
        arguments = mapOf(
            "candidate" to collatorId,
            "less" to amount
        )
    )
}

fun ExtrinsicBuilder.executeDelegationRequest(
    delegator: AccountId,
    collatorId: AccountId
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = "execute_delegation_request",
        arguments = mapOf(
            "delegator" to delegator,
            "candidate" to collatorId
        )
    )
}

fun ExtrinsicBuilder.cancelDelegationRequest(
    collatorId: AccountId
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = "cancel_delegation_request",
        arguments = mapOf(
            "candidate" to collatorId,
        )
    )
}
