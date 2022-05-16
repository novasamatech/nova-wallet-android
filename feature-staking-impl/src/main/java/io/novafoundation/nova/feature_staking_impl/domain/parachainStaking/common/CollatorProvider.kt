package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getIdentityFromId
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorsSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.minimumStake
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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
) : CollatorProvider {

    override suspend fun electedCollators(chainId: ChainId): List<Collator> {
        val snapshots = currentRoundRepository.collatorsSnapshotInCurrentRound(chainId)
        val allAccountsIds = snapshots.keys.toList()

        val identities = identityRepository.getIdentitiesFromIds(chainId, allAccountsIds)

        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val maxRewardableDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        return snapshots.map { (accountIdHex, collatorSnapshot) ->
            Collator(
                accountIdHex = accountIdHex,
                identity = identities[accountIdHex],
                snapshot = collatorSnapshot,
                minimumStakeToGetRewards = collatorSnapshot.minimumStake(systemForcedMinimumStake, maxRewardableDelegatorsPerCollator)
            )
        }
    }

    override suspend fun electedCollator(chainId: ChainId, collatorId: AccountId): Collator? {
        val snapshot = currentRoundRepository.collatorSnapshotInCurrentRound(chainId, collatorId) ?: return null

        val accountIdHex = collatorId.toHexString()
        val identity = identityRepository.getIdentityFromId(chainId, accountIdHex)
        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val maxRewardableDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        return Collator(
            accountIdHex = accountIdHex,
            identity = identity,
            snapshot = snapshot,
            minimumStakeToGetRewards = snapshot.minimumStake(systemForcedMinimumStake, maxRewardableDelegatorsPerCollator)
        )
    }
}
