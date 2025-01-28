package io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class MythosReleaseRequestLimitNotReachedValidationFactory @Inject constructor(
    private val stakingRepository: MythosStakingRepository,
    private val userStakeRepository: MythosUserStakeRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) {

    context(UnbondMythosValidationSystemBuilder)
    fun releaseRequestsLimitNotReached() {
        validate(ReleaseRequestLimitNotReachedValidation(
            stakingRepository = stakingRepository,
            userStakeRepository = userStakeRepository,
            accountRepository = accountRepository,
            chainRegistry = chainRegistry
        ))
    }
}

private class ReleaseRequestLimitNotReachedValidation(
    private val stakingRepository: MythosStakingRepository,
    private val userStakeRepository: MythosUserStakeRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
): UnbondMythosValidation {

    override suspend fun validate(value: UnbondMythosStakingValidationPayload): ValidationStatus<UnbondMythosStakingValidationFailure> {
        val chain = chainRegistry.getChain(value.chainId)
        val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain).intoKey()

        val releaseQueues = userStakeRepository.releaseQueues(chain.id, accountId)
        val limit = stakingRepository.maxReleaseRequests(chain.id)

        return (releaseQueues.size < limit) isTrueOrError {
            UnbondMythosStakingValidationFailure.ReleaseRequestsLimitReached(limit)
        }
    }
}
