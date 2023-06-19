package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.state.chain

class AccountIsNotControllerValidation<P, E>(
    private val stakingRepository: StakingRepository,
    private val controllerAddressProducer: (P) -> String,
    private val sharedState: StakingSharedState,
    private val errorProducer: (P) -> E,
) : Validation<P, E> {
    override suspend fun validate(value: P): ValidationStatus<E> {
        val controllerAddress = controllerAddressProducer(value)
        val chain = sharedState.chain()
        val ledger = stakingRepository.ledger(sharedState.chainId(), chain.accountIdOf(controllerAddress))

        return if (ledger == null) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, errorProducer(value))
        }
    }
}
