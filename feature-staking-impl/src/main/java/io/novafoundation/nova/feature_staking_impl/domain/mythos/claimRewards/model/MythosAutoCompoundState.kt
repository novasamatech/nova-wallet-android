@file:OptIn(ExperimentalContracts::class)

package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface MythosAutoCompoundState {

    companion object {

        fun from(
            userStake: Balance,
            threshold: Balance,
            userCompoundFraction: Fraction,
            userModifiedStateInPast: Boolean,
        ): MythosAutoCompoundState {
            return when {
                userStake < threshold -> NotConfigurable(threshold)
                // When on-chain state is set, we are sure user has set the percentage
                !userCompoundFraction.isZero -> Configurable.Restake
                // Otherwise, we consult with our local knowledge
                !userModifiedStateInPast -> Configurable.NotSet

                else -> Configurable.Transferable
            }
        }
    }

    /**
     * User is not able to configure auto-compounding due to stake threshold
     * Rewards will always be transferable
     */
    class NotConfigurable(val threshold: Balance) : MythosAutoCompoundState

    sealed interface Configurable : MythosAutoCompoundState {

        companion object {

            fun fromSelected(shouldRestake: Boolean): Configurable {
                return if (shouldRestake) Restake else Transferable
            }
        }

        /**
         * User has not set the state manually even once (as the our app is aware)
         */
        object NotSet : Configurable

        object Restake : Configurable

        object Transferable : Configurable
    }
}

fun MythosAutoCompoundState.isConfigurable(): Boolean {
    contract {
        returns(true) implies (this@isConfigurable is MythosAutoCompoundState.Configurable)
        returns(false) implies (this@isConfigurable is MythosAutoCompoundState.NotConfigurable)
    }

    return this is MythosAutoCompoundState.Configurable
}
