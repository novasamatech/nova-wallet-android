package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.accountIsStash
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.state.chain

class StashOnlyIsAllowedValidation<P, E>(
    val accountRepository: AccountRepository,
    val stakingState: (P) -> StakingState,
    val sharedState: StakingSharedState,
    val errorProducer: (stashAddress: String, stashAccount: MetaAccount?) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val stakingState = stakingState(value)
        if (stakingState !is StakingState.Stash) return ValidationStatus.Valid()

        return if (stakingState.accountIsStash()) {
            ValidationStatus.Valid()
        } else {
            val chain = sharedState.chain()
            val stashMetaAccount = accountRepository.findMetaAccount(stakingState.stashId, chain.id)
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, errorProducer(stakingState.stashAddress, stashMetaAccount))
        }
    }
}
