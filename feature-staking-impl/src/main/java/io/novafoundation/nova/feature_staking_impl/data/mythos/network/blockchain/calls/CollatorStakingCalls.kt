package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

@JvmInline
value class CollatorStakingCalls(val builder: ExtrinsicBuilder)

val ExtrinsicBuilder.collatorStaking: CollatorStakingCalls
    get() = CollatorStakingCalls(this)

fun CollatorStakingCalls.lock(amount: Balance) {
    builder.call(
        moduleName = Modules.COLLATOR_STAKING,
        callName = "lock",
        arguments = mapOf(
            "amount" to amount
        )
    )
}

data class StakingIntent(val candidate: AccountIdKey, val stake: Balance) {

    companion object {

        fun zero(candidate: AccountIdKey) = StakingIntent(candidate, Balance.ZERO)
    }
}

fun CollatorStakingCalls.stake(intents: List<StakingIntent>) {
    val targets = intents.map(StakingIntent::toEncodableInstance)

    builder.call(
        moduleName = Modules.COLLATOR_STAKING,
        callName = "stake",
        arguments = mapOf(
            "targets" to targets
        )
    )
}

fun CollatorStakingCalls.unstakeFrom(collatorId: AccountIdKey) {
    builder.call(
        moduleName = Modules.COLLATOR_STAKING,
        callName = "unstake_from",
        arguments = mapOf(
            "account" to collatorId.value
        )
    )
}

fun CollatorStakingCalls.unlock(amount: Balance) {
    builder.call(
        moduleName = Modules.COLLATOR_STAKING,
        callName = "unlock",
        arguments = mapOf(
            "maybe_amount" to amount
        )
    )
}

private fun StakingIntent.toEncodableInstance(): Any {
    return structOf(
        "candidate" to candidate.value,
        "stake" to stake
    )
}
