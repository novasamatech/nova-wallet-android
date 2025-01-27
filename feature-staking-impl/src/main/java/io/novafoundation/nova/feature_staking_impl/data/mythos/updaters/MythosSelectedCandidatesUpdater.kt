package io.novafoundation.nova.feature_staking_impl.data.mythos.updaters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.onEachLatest
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.storage.insert
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.candidateStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MythosSelectedCandidatesUpdater(
    override val scope: AccountUpdateScope,
    private val stakingSharedState: StakingSharedState,
    private val storageCache: StorageCache,
    private val mythosUserStakeRepository: MythosUserStakeRepository,
    private val remoteStorageDataSource: StorageDataSource,
) : Updater<MetaAccount> {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount
    ): Flow<Updater.SideEffect> {
        val chain = stakingSharedState.chain()
        val accountId = scopeValue.accountIdIn(chain) ?: return emptyFlow()

        return mythosUserStakeRepository.userStakeFlow(chain.id, accountId).onEachLatest { userStake ->
            if (userStake == null) return@onEachLatest

            syncUserDelegations(chain.id, userStake, accountId.intoKey())
        }.noSideAffects()
    }

    private suspend fun syncUserDelegations(
        chainId: ChainId,
        userStake: UserStakeInfo,
        userAccountId: AccountIdKey,
    ) {
        val allKeys = userStake.candidates.map { it to userAccountId }

        val entries = remoteStorageDataSource.query(chainId) {
            metadata.collatorStaking.candidateStake.entriesRaw(allKeys)
        }

        storageCache.insert(entries, chainId)
    }
}
