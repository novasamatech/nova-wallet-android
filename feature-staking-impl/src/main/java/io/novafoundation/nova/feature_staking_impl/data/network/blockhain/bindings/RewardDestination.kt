package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage

private const val TYPE_STAKED = "Staked"
private const val TYPE_ACCOUNT = "Account"

fun bindRewardDestination(rewardDestination: RewardDestination) = when (rewardDestination) {
    is RewardDestination.Restake -> DictEnum.Entry(TYPE_STAKED, null)
    is RewardDestination.Payout -> DictEnum.Entry(TYPE_ACCOUNT, rewardDestination.targetAccountId)
}

fun bindRewardDestination(
    scale: String,
    runtime: RuntimeSnapshot,
    stashId: AccountId,
    controllerId: AccountId,
): RewardDestination {
    val type = runtime.metadata.staking().storage("Payee").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<DictEnum.Entry<*>>()

    return when (dynamicInstance.name) {
        TYPE_STAKED -> RewardDestination.Restake
        TYPE_ACCOUNT -> RewardDestination.Payout(dynamicInstance.value.cast())
        "Stash" -> RewardDestination.Payout(stashId)
        "Controller" -> RewardDestination.Payout(controllerId)
        else -> incompatible()
    }
}
