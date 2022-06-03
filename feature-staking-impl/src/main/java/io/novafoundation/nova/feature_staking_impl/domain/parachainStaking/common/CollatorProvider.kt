package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.minimumStakeToGetRewards
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorsSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal

interface CollatorProvider {

    sealed class CollatorSource {

        object Elected : CollatorSource()

        class Custom(val collatorIdsHex: Collection<String>) : CollatorSource()
    }

    suspend fun getCollators(
        chainId: ChainId,
        collatorSource: CollatorSource,
        cachedSnapshots: AccountIdMap<CollatorSnapshot>? = null
    ): List<Collator>
}

class RealCollatorProvider(
    private val identityRepository: IdentityRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
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

        val requestedCollatorIdsHex = when (collatorSource) {
            is CollatorSource.Custom -> collatorSource.collatorIdsHex.toSet()
            CollatorSource.Elected -> snapshots.keys
        }
        val requestedCollatorIds = requestedCollatorIdsHex.map { it.fromHex() }

        val candidateMetadatas = candidatesRepository.getCandidatesMetadata(chainId, requestedCollatorIds)
        val identities = identityRepository.getIdentitiesFromIds(chainId, requestedCollatorIdsHex)

        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val rewardCalculator = rewardCalculatorFactory.create(chainId, snapshots)

        return requestedCollatorIdsHex.map { accountIdHex ->
            val collatorSnapshot = snapshots[accountIdHex]
            val candidateMetadata = candidateMetadatas.getValue(accountIdHex)

            Collator(
                accountIdHex = accountIdHex,
                address = chain.addressOf(accountIdHex.fromHex()),
                identity = identities[accountIdHex],
                snapshot = collatorSnapshot,
                minimumStakeToGetRewards = candidateMetadata.minimumStakeToGetRewards(systemForcedMinimumStake),
                candidateMetadata = candidateMetadata,
                apr = rewardCalculator.getAprOrEstimate(accountIdHex, candidateMetadatas)
            )
        }
    }

    private fun ParachainStakingRewardCalculator.getAprOrEstimate(
        accountIdHex: String,
        missingCollatorMetadatas: AccountIdMap<CandidateMetadata>
    ): BigDecimal {
        return collatorApr(accountIdHex) ?: estimateApr(missingCollatorMetadatas.getValue(accountIdHex).totalCounted)
    }
}

suspend fun CollatorProvider.getCollator(chainId: ChainId, collatorId: AccountId): Collator = getCollators(
    chainId = chainId,
    collatorSource = CollatorSource.Custom(listOf(collatorId.toHexString())),
).first()
