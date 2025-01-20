package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythDelegation
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.hasActiveValidators
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosLocks
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.total
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class MythosDelegatorState {

    object NotStarted : MythosDelegatorState()

    sealed class Locked(
        val locks: MythosLocks
    ) : MythosDelegatorState() {

        class NotDelegating(
            locks: MythosLocks
        ) : Locked(locks)

        class Delegating(
            val userStakeInfo: UserStakeInfo,
            val stakeByCollator: Map<AccountIdKey, MythDelegation>,
            locks: MythosLocks
        ) : Locked(locks)
    }
}

@OptIn(ExperimentalContracts::class)
fun MythosDelegatorState.isDelegating(): Boolean {
    contract {
        returns(true) implies (this@isDelegating is MythosDelegatorState.Locked.Delegating)
    }

    return this is MythosDelegatorState.Locked.Delegating
}

@OptIn(ExperimentalContracts::class)
fun MythosDelegatorState.isNotStarted(): Boolean {
    contract {
        returns(true) implies (this@isNotStarted is MythosDelegatorState.NotStarted)
    }

    return this is MythosDelegatorState.NotStarted
}

fun MythosDelegatorState.stakeByCollator(): Map<AccountIdKey, Balance> {
    return when (this) {
        is MythosDelegatorState.Locked.Delegating -> stakeByCollator.mapValues { it.value.stake }
        MythosDelegatorState.NotStarted, is MythosDelegatorState.Locked.NotDelegating -> emptyMap()
    }
}

fun MythosDelegatorState.hasActiveValidators(sessionValidators: SessionValidators): Boolean {
    return when (this) {
        is MythosDelegatorState.Locked.Delegating -> userStakeInfo.hasActiveValidators(sessionValidators)
        MythosDelegatorState.NotStarted, is MythosDelegatorState.Locked.NotDelegating -> false
    }
}

val MythosDelegatorState.activeStake: Balance
    get() = when (this) {
        is MythosDelegatorState.Locked.Delegating -> userStakeInfo.balance

        is MythosDelegatorState.Locked.NotDelegating,
        MythosDelegatorState.NotStarted -> Balance.ZERO
    }

val MythosDelegatorState.totalLocked: Balance
    get() = when (this) {
        is MythosDelegatorState.Locked -> locks.total
        MythosDelegatorState.NotStarted -> Balance.ZERO
    }

fun MythosDelegatorState.stakeableBalance(asset: Asset): Balance {
    return (asset.freeInPlanks - totalLocked).atLeastZero()
}

fun MythosDelegatorState.delegationAmountTo(collator: AccountIdKey): Balance {
    return when (this) {
        is MythosDelegatorState.Locked.Delegating -> stakeByCollator[collator]?.stake.orZero()
        is MythosDelegatorState.Locked.NotDelegating, MythosDelegatorState.NotStarted -> Balance.ZERO
    }
}
