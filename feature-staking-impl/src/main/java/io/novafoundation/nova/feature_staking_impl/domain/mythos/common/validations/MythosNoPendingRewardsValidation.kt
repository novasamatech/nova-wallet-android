package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.validations

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.isNotStarted
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import javax.inject.Inject

@FeatureScope
class MythosNoPendingRewardsValidationFactory @Inject constructor(
    private val userStakeRepository: MythosUserStakeRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) {

    context(ValidationSystemBuilder<P, E>)
    fun <P, E> noPendingRewards(
        delegatorState: (P) -> MythosDelegatorState,
        chainId: (P) -> ChainId,
        error: () -> E
    ) {
        validate(
            MythosNoPendingRewardsValidation(
                userStakeRepository = userStakeRepository,
                chainRegistry = chainRegistry,
                accountRepository = accountRepository,
                delegatorState = delegatorState,
                chainId = chainId,
                errorProducer = error
            )
        )
    }
}

class MythosNoPendingRewardsValidation<P, E>(
    private val userStakeRepository: MythosUserStakeRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,

    private val delegatorState: (P) -> MythosDelegatorState,
    private val chainId: (P) -> ChainId,
    private val errorProducer: () -> E
) : Validation<P, E> {

    override suspend fun validate(
        value: P
    ): ValidationStatus<E> {
        // fast path - nothing to check if not started
        if (delegatorState(value).isNotStarted()) return valid()

        val chain = chainRegistry.getChain(chainId(value))
        val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain).intoKey()

        val shouldClaimRewards = userStakeRepository.shouldClaimRewards(chain.id, accountId)

        return shouldClaimRewards isFalseOrError {
            errorProducer()
        }
    }
}
