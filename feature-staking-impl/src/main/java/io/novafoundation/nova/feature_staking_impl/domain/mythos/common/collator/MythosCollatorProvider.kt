package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Fraction
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

        val collatorStakes = mythosSharedComputation.get().candidateInfos(chainId)

        val accountIdsRaw = requestedCollatorIds.map { it.value }
        val identities = identityRepository.getIdentitiesFromIds(accountIdsRaw, chainId)

        return requestedCollatorIds.mapNotNull { collatorId ->
            // if elected does not have a stake entry, it means it is invulnerable => ignore it
            val collatorStake = collatorStakes[collatorId] ?: return@mapNotNull null

            MythosCollator(
                accountId = collatorId,
                identity = identities[collatorId],
                totalStake = collatorStake.stake,
                delegators = collatorStake.stakers,
                // TODO APY calculation
                apy = Fraction.ZERO
            )
        }
    }

    context(ComputationalScope)
    private suspend fun MythosCollatorSource.requestedCollatorIds(chainId: ChainId): Collection<AccountIdKey> {
        return when (this) {
            is MythosCollatorSource.Custom -> collatorIds
            MythosCollatorSource.ElectedCandidates -> mythosSharedComputation.get().sessionValidators(chainId)
        }
    }
}
