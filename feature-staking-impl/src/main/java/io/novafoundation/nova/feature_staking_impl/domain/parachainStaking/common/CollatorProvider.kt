package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getIdentityFromId
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorsSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.minimumStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface CollatorProvider {

    sealed class CollatorSource {

        object Elected: CollatorSource()

        class Custom(val collatorIdsHex: Collection<String>): CollatorSource()
    }

    suspend fun getCollators(
        chainId: ChainId,
        collatorSource: CollatorSource,
        cachedSnapshots: AccountIdMap<CollatorSnapshot>? = null
    ): List<Collator>

    suspend fun electedCollator(chainId: ChainId, collatorId: AccountId): Collator?
}

class RealCollatorProvider(
    private val identityRepository: IdentityRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val chainRegistry: ChainRegistry,
) : CollatorProvider {

    override suspend fun getCollators(
        chainId: ChainId,
        collatorSource: CollatorSource,
        cachedSnapshots: AccountIdMap<CollatorSnapshot>?
    ): List<Collator> {
        val chain = chainRegistry.getChain(chainId)

        val snapshots = cachedSnapshots ?: currentRoundRepository.collatorsSnapshotInCurrentRound(chainId)

        val requestedCollatorIds = when(collatorSource) {
            is CollatorSource.Custom -> collatorSource.collatorIdsHex
            CollatorSource.Elected -> snapshots.keys
        }

        val identities = identityRepository.getIdentitiesFromIds(chainId, requestedCollatorIds)

        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val rewardCalculator = rewardCalculatorFactory.create(chainId, snapshots)
        val maxRewardedDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        return requestedCollatorIds.map { accountIdHex ->
            val collatorSnapshot = snapshots[accountIdHex]

            Collator(
                accountIdHex = accountIdHex,
                address = chain.addressOf(accountIdHex.fromHex()),
                identity = identities[accountIdHex],
                snapshot = collatorSnapshot,
                minimumStakeToGetRewards = collatorSnapshot?.minimumStake(systemForcedMinimumStake, maxRewardedDelegatorsPerCollator),
                apr = rewardCalculator.collatorApr(accountIdHex)
            )
        }
    }

    override suspend fun electedCollator(chainId: ChainId, collatorId: AccountId): Collator? {
        val snapshots = currentRoundRepository.collatorsSnapshotInCurrentRound(chainId)
        val collatorSnapshot = snapshots[collatorId.toHexString()] ?: return null

        val chain = chainRegistry.getChain(chainId)

        val collatorIdHex = collatorId.toHexString()
        val identity = identityRepository.getIdentityFromId(chainId, collatorIdHex)
        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val maxRewardableDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        val rewardCalculator = rewardCalculatorFactory.create(chainId, snapshots)

        return Collator(
            accountIdHex = collatorIdHex,
            address = chain.addressOf(collatorId),
            identity = identity,
            snapshot = collatorSnapshot,
            minimumStakeToGetRewards = collatorSnapshot.minimumStake(systemForcedMinimumStake, maxRewardableDelegatorsPerCollator),
            apr = rewardCalculator.collatorApr(collatorIdHex)!!
        )
    }
}
