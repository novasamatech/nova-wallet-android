package io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.isNotStarted
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class MythosNoPendingRewardsValidationFactory @Inject constructor(
    private val userStakeRepository: MythosUserStakeRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) {

    context(StartMythosStakingValidationSystemBuilder)
    fun noPendingRewards() {
        validate(
            MythosNoPendingRewardsValidation(
                userStakeRepository = userStakeRepository,
                chainRegistry = chainRegistry,
                accountRepository = accountRepository
            )
        )
    }
}

class MythosNoPendingRewardsValidation(
    private val userStakeRepository: MythosUserStakeRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : StartMythosStakingValidation {

    override suspend fun validate(
        value: StartMythosStakingValidationPayload
    ): ValidationStatus<StartMythosStakingValidationFailure> {
        // fast path - nothing to check if not started
        if (value.delegatorState.isNotStarted()) return valid()

        val chain = chainRegistry.getChain(value.chainId)
        val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain).intoKey()

        val shouldClaimRewards = userStakeRepository.shouldClaimRewards(chain.id, accountId)

        return shouldClaimRewards isFalseOrError {
            StartMythosStakingValidationFailure.HasNotClaimedRewards
        }
    }
}
