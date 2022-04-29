package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import kotlinx.coroutines.flow.first

private const val UNLOCKING_LIMIT = 32

class UnbondingRequestsLimitValidation<P, E>(
    val stakingRepository: StakingRepository,
    val stashStateProducer: (P) -> StakingState.Stash,
    val errorProducer: (limit: Int) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val ledger = stakingRepository.ledgerFlow(stashStateProducer(value)).first()

        return if (ledger.unlocking.size < UNLOCKING_LIMIT) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, errorProducer(UNLOCKING_LIMIT))
        }
    }
}
