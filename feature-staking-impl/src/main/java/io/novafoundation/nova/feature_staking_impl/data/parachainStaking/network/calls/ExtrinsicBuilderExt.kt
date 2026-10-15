package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.firstExistingCallName
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import java.math.BigInteger

// Each helper probes runtime metadata for an EWX (AvN-renamed) call name
// first, then falls back to the Moonbeam name. The argument map is built
// for whichever call shape the chain actually exposes.

fun ExtrinsicBuilder.delegate(
    candidate: AccountId,
    amount: Balance,
    candidateDelegationCount: BigInteger,
    delegationCount: BigInteger
): ExtrinsicBuilder {
    val parachainStaking = runtime.metadata.parachainStaking()
    val callName = parachainStaking.firstExistingCallName(
        "nominate",
        "delegate_with_auto_compound",
        "delegate"
    )

    val arguments: Map<String, Any?> = when (callName) {
        "nominate" -> mapOf(
            "candidate" to candidate,
            "amount" to amount,
            "candidate_nomination_count" to candidateDelegationCount,
            "nomination_count" to delegationCount
        )
        "delegate_with_auto_compound" -> mapOf(
            "candidate" to candidate,
            "amount" to amount,
            "auto_compound" to BigInteger.ZERO,
            "candidate_delegation_count" to candidateDelegationCount,
            "candidate_auto_compounding_delegation_count" to BigInteger.ZERO,
            "delegation_count" to delegationCount
        )
        else -> mapOf(
            "candidate" to candidate,
            "amount" to amount,
            "candidate_delegation_count" to candidateDelegationCount,
            "delegation_count" to delegationCount
        )
    }

    return call(moduleName = Modules.PARACHAIN_STAKING, callName = callName, arguments = arguments)
}

fun ExtrinsicBuilder.delegatorBondMore(
    candidate: AccountId,
    amount: Balance,
): ExtrinsicBuilder {
    val callName = runtime.metadata.parachainStaking()
        .firstExistingCallName("bond_extra", "delegator_bond_more")

    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = callName,
        arguments = mapOf(
            "candidate" to candidate,
            "more" to amount
        )
    )
}

fun ExtrinsicBuilder.scheduleRevokeDelegation(
    collatorId: AccountId
): ExtrinsicBuilder {
    val callName = runtime.metadata.parachainStaking()
        .firstExistingCallName("schedule_revoke_nomination", "schedule_revoke_delegation")

    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = callName,
        arguments = mapOf("collator" to collatorId)
    )
}

fun ExtrinsicBuilder.scheduleBondLess(
    collatorId: AccountId,
    amount: Balance,
): ExtrinsicBuilder {
    val callName = runtime.metadata.parachainStaking()
        .firstExistingCallName("schedule_nominator_unbond", "schedule_delegator_bond_less")

    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = callName,
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
    val callName = runtime.metadata.parachainStaking()
        .firstExistingCallName("execute_nomination_request", "execute_delegation_request")

    val delegatorKey = if (callName == "execute_nomination_request") "nominator" else "delegator"

    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = callName,
        arguments = mapOf(
            delegatorKey to delegator,
            "candidate" to collatorId
        )
    )
}

fun ExtrinsicBuilder.cancelDelegationRequest(
    collatorId: AccountId
): ExtrinsicBuilder {
    val callName = runtime.metadata.parachainStaking()
        .firstExistingCallName("cancel_nomination_request", "cancel_delegation_request")

    return call(
        moduleName = Modules.PARACHAIN_STAKING,
        callName = callName,
        arguments = mapOf("candidate" to collatorId)
    )
}
