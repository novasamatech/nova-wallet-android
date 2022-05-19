package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getIdentityFromId
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorsSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
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

    suspend fun electedCollators(chainId: ChainId): List<Collator>

    suspend fun electedCollator(chainId: ChainId, collatorId: AccountId): Collator?
}

class RealCollatorProvider(
    private val identityRepository: IdentityRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val chainRegistry: ChainRegistry,
) : CollatorProvider {

    override suspend fun electedCollators(chainId: ChainId): List<Collator> {
        val chain = chainRegistry.getChain(chainId)

        val snapshots = currentRoundRepository.collatorsSnapshotInCurrentRound(chainId)
        val allAccountsIds = snapshots.keys.toList()

        val identities = identityRepository.getIdentitiesFromIds(chainId, allAccountsIds)

        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val rewardCalculator = rewardCalculatorFactory.create(chainId, snapshots)
        val maxRewardableDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        return snapshots.map { (accountIdHex, collatorSnapshot) ->
            Collator(
                accountIdHex = accountIdHex,
                address = chain.addressOf(accountIdHex.fromHex()),
                identity = identities[accountIdHex],
                snapshot = collatorSnapshot,
                minimumStakeToGetRewards = collatorSnapshot.minimumStake(systemForcedMinimumStake, maxRewardableDelegatorsPerCollator),
                apr = rewardCalculator.collatorApr(accountIdHex)!!
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
