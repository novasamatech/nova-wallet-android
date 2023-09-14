package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.minimumStakeToGetRewards
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.collatorsSnapshotInCurrentRound
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface CollatorProvider {

    sealed class CollatorSource {

        object Elected : CollatorSource()

        class Custom(val collatorIdsHex: Collection<String>) : CollatorSource()
    }

    suspend fun getCollators(
        stakingOption: StakingOption,
        collatorSource: CollatorSource,
        cachedSnapshots: AccountIdMap<CollatorSnapshot>? = null
    ): List<Collator>
}

class RealCollatorProvider(
    private val identityRepository: OnChainIdentityRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val chainRegistry: ChainRegistry,
) : CollatorProvider {

    override suspend fun getCollators(
        stakingOption: StakingOption,
        collatorSource: CollatorSource,
        cachedSnapshots: AccountIdMap<CollatorSnapshot>?
    ): List<Collator> {
        val chainAsset = stakingOption.assetWithChain.asset
        val chainId = chainAsset.chainId
        val chain = chainRegistry.getChain(chainAsset.chainId)

        val snapshots = cachedSnapshots ?: currentRoundRepository.collatorsSnapshotInCurrentRound(chainId)

        val requestedCollatorIdsHex = when (collatorSource) {
            is CollatorSource.Custom -> collatorSource.collatorIdsHex.toSet()
            CollatorSource.Elected -> snapshots.keys
        }
        val requestedCollatorIds = requestedCollatorIdsHex.map { it.fromHex() }

        val candidateMetadatas = candidatesRepository.getCandidatesMetadata(chainId, requestedCollatorIds)
        val identities = identityRepository.getIdentitiesFromIds(requestedCollatorIds, chainId)

        val systemForcedMinimumStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val rewardCalculator = rewardCalculatorFactory.create(stakingOption, snapshots)

        return requestedCollatorIdsHex.map { accountIdHex ->
            val collatorSnapshot = snapshots[accountIdHex]
            val candidateMetadata = candidateMetadatas.getValue(accountIdHex)
            val accountId = accountIdHex.fromHex()

            Collator(
                accountIdHex = accountIdHex,
                address = chain.addressOf(accountId),
                identity = identities[accountId],
                snapshot = collatorSnapshot,
                minimumStakeToGetRewards = candidateMetadata.minimumStakeToGetRewards(systemForcedMinimumStake),
                candidateMetadata = candidateMetadata,
                apr = rewardCalculator.collatorApr(accountIdHex)
            )
        }
    }
}

suspend fun CollatorProvider.getCollator(stakingOption: StakingOption, collatorId: AccountId): Collator = getCollators(
    stakingOption = stakingOption,
    collatorSource = CollatorSource.Custom(listOf(collatorId.toHexString())),
).first()
