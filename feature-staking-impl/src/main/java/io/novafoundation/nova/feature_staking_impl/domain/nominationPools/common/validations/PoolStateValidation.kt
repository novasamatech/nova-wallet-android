package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class PoolStateValidationFactory(
    private val poolStateRepository: NominationPoolStateRepository,
) {

    context(ValidationSystemBuilder<T, S>)
    fun <T, S> validatePoolState(
        poolId: (T) -> PoolId,
        chainId: (T) -> ChainId,
        stateValid: (PoolState) -> Boolean,
        error: (T) -> S
    ) {
        validate(PoolStateValidation(poolStateRepository, poolId, chainId, stateValid, error))
    }
}

context(ValidationSystemBuilder<T, S>)
fun <T, S> PoolStateValidationFactory.validateNotDestroying(
    poolId: (T) -> PoolId,
    chainId: (T) -> ChainId,
    error: (T) -> S
) {
    validatePoolState(
        poolId = poolId,
        chainId  = chainId,
        stateValid = { it != PoolState.Destroying  },
        error = error
    )
}

class PoolStateValidation<T, S>(
    private val poolStateRepository: NominationPoolStateRepository,
    private val poolId: (T) -> PoolId,
    private val chainId: (T) -> ChainId,
    private val stateValid: (PoolState) -> Boolean,
    private val error: (T) -> S
): Validation<T, S> {

    override suspend fun validate(value: T): ValidationStatus<S> {
        val bondedPool = poolStateRepository.getParticipatingBondedPool(poolId(value), chainId(value))
        val isStateValid = stateValid(bondedPool.state)

        return isStateValid isTrueOrError { error(value) }
    }
}
