package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
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

    class Staked(
        val userStakeInfo: UserStakeInfo,
        val stakeByCollator: Map<AccountIdKey, MythDelegation>,
        val locks: MythosLocks
    ) : MythosDelegatorState()
}

@OptIn(ExperimentalContracts::class)
fun MythosDelegatorState.hasStakedCollators(): Boolean {
    contract {
        returns(true) implies (this@hasStakedCollators is MythosDelegatorState.Staked)
    }

    return this is MythosDelegatorState.Staked && stakeByCollator.isNotEmpty()
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
        is MythosDelegatorState.Staked -> stakeByCollator.mapValues { it.value.stake }
        MythosDelegatorState.NotStarted -> emptyMap()
    }
}

fun MythosDelegatorState.stakedCollatorsCount(): Int {
    return when (this) {
        is MythosDelegatorState.Staked -> userStakeInfo.candidates.size
        MythosDelegatorState.NotStarted -> 0
    }
}

fun MythosDelegatorState.hasActiveValidators(sessionValidators: SessionValidators): Boolean {
    return when (this) {
        is MythosDelegatorState.Staked -> userStakeInfo.hasActiveValidators(sessionValidators)
        MythosDelegatorState.NotStarted -> false
    }
}

val MythosDelegatorState.activeStake: Balance
    get() = when (this) {
        is MythosDelegatorState.Staked -> userStakeInfo.balance

        MythosDelegatorState.NotStarted -> Balance.ZERO
    }

fun MythosDelegatorState.stakeableBalance(asset: Asset, currentBlockNumber: BlockNumber): Balance {
    return when (this) {
        // Since there is no staking yet, we can stake the whole free balance
        MythosDelegatorState.NotStarted -> asset.freeInPlanks

        // We can stake from not-yet-staked balance + can restake unused part of CollatorStaking::Staking freeze
        is MythosDelegatorState.Staked -> {
            val fromNonLocked = asset.freeInPlanks - locks.total
            val fromLocked = locks.staked - userStakeInfo.balance - userStakeInfo.restrictedFromRestake(currentBlockNumber)

            fromNonLocked + fromLocked
        }
    }
}

fun UserStakeInfo.restrictedFromRestake(currentBlockNumber: BlockNumber): Balance {
    return if (maybeLastUnstake != null && currentBlockNumber < maybeLastUnstake.availableForRestakeAt) {
        maybeLastUnstake.amount
    } else {
        Balance.ZERO
    }
}

fun MythosDelegatorState.delegationAmountTo(collator: AccountIdKey): Balance {
    return when (this) {
        is MythosDelegatorState.Staked -> stakeByCollator[collator]?.stake.orZero()
        MythosDelegatorState.NotStarted -> Balance.ZERO
    }
}
