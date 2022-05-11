package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorsSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.minimumStake
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface CollatorProvider {

    suspend fun electedCollators(chainId: ChainId): List<Collator>
}

class RealCollatorProvider(
    private val identityRepository: IdentityRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
): CollatorProvider {

    override suspend fun electedCollators(chainId: ChainId): List<Collator> {
        val snapshots = currentRoundRepository.collatorsSnapshotInCurrentRound(chainId)
        val allAccountsIds = snapshots.keys.toList()

        val identities = identityRepository.getIdentitiesFromIds(chainId, allAccountsIds)

        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)

        return snapshots.map { (accountIdHex, collatorSnapshot) ->
            Collator(
                accountIdHex = accountIdHex,
                identity = identities[accountIdHex],
                snapshot = collatorSnapshot,
                minimumStake = collatorSnapshot.minimumStake(systemForcedMinimumStake)
            )
        }
    }
}
