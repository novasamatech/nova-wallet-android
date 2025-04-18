package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider.MythosCollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.sessionValidators
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import javax.inject.Inject

interface MythosCollatorProvider {

    sealed class MythosCollatorSource {

        /**
         * Elected collators that are not invulnerable
         */
        object ElectedCandidates : MythosCollatorSource()

        class Custom(val collatorIds: Collection<AccountIdKey>) : MythosCollatorSource()
    }

    context(ComputationalScope)
    suspend fun getCollators(
        stakingOption: StakingOption,
        collatorSource: MythosCollatorSource,
    ): List<MythosCollator>
}

@FeatureScope
class RealMythosCollatorProvider @Inject constructor(
    private val mythosSharedComputation: dagger.Lazy<MythosSharedComputation>,
    private val identityRepository: OnChainIdentityRepository
) : MythosCollatorProvider {

    context(ComputationalScope)
    override suspend fun getCollators(
        stakingOption: StakingOption,
        collatorSource: MythosCollatorSource
    ): List<MythosCollator> {
        val chainId = stakingOption.chain.id
        val requestedCollatorIds = collatorSource.requestedCollatorIds(chainId)

        if (requestedCollatorIds.isEmpty()) return emptyList()

        val sharedComputation = mythosSharedComputation.get()

        val collatorStakes = sharedComputation.candidateInfos(chainId)

        val accountIdsRaw = requestedCollatorIds.map { it.value }
        val identities = identityRepository.getIdentitiesFromIds(accountIdsRaw, chainId)

        val rewardCalculator = sharedComputation.rewardCalculator(chainId)

        return requestedCollatorIds.map { collatorId ->
            val collatorStake = collatorStakes[collatorId]

            MythosCollator(
                accountId = collatorId,
                identity = identities[collatorId],
                totalStake = collatorStake?.stake.orZero(),
                delegators = collatorStake?.stakers.orZero(),
                apr = rewardCalculator.collatorApr(collatorId)
            )
        }
    }

    context(ComputationalScope)
    private suspend fun MythosCollatorSource.requestedCollatorIds(chainId: ChainId): Collection<AccountIdKey> {
        return when (this) {
            is MythosCollatorSource.Custom -> collatorIds
            MythosCollatorSource.ElectedCandidates -> getElectedCandidates(chainId)
        }
    }

    context(ComputationalScope)
    private suspend fun getElectedCandidates(chainId: ChainId): Collection<AccountIdKey> {
        val sessionValidators = mythosSharedComputation.get().sessionValidators(chainId)
        val invulnerables = mythosSharedComputation.get().getInvulnerableCollators(chainId)

        return sessionValidators - invulnerables
    }
}
