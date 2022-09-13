package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.turing

import io.novafoundation.nova.common.utils.automationTime
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.storage.insertPrefixEntries
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.ParachainStakingUpdater
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

class TuringAutomationTasksUpdater(
    private val stakingSharedState: StakingSharedState,
    private val storageCache: StorageCache,
    private val remoteStorageSource: StorageDataSource,
    private val walletRepository: WalletRepository,
    override val scope: AccountUpdateScope,
) : ParachainStakingUpdater {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val (chain, chainAsset) = stakingSharedState.chainAndAsset()
        val metaAccount = scope.getAccount()
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        return walletRepository.assetFlow(metaAccount.id, chainAsset)
            .distinctUntilChangedBy { it.totalInPlanks }
            .onEach {
                remoteStorageSource.query(chain.id) {
                    val storageEntry = runtime.metadata.automationTime().storage("AccountTasks")
                    val entries = storageEntry.entriesRaw(accountId)
                    val storagePrefix = storageEntry.storageKey(runtime, accountId)

                    storageCache.insertPrefixEntries(entries, prefix = storagePrefix, chainId = chain.id)
                }
            }.noSideAffects()
    }
}
