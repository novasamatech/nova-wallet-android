package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.turing

import io.novafoundation.nova.common.utils.automationTime
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.storage.insertPrefixEntries
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters.ParachainStakingUpdater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.withIndex

class TuringAutomationTasksUpdater(
    private val stakingSharedState: StakingSharedState,
    private val storageCache: StorageCache,
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    override val scope: AccountUpdateScope,
) : ParachainStakingUpdater {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val chain = stakingSharedState.chain()
        val metaAccount = scope.getAccount()
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountKey = runtime.metadata.system().storage("Account").storageKey(runtime, accountId)

        return storageSubscriptionBuilder.subscribe(accountKey)
            .withIndex()
            .mapLatest { (index, change) ->
                val expectedAccountValue = change.value
                val isChange = index > 0

                remoteStorageSource.query(chain.id) {
                    if (isChange) {
                       awaitSameAccountValue(accountId, expectedAccountValue)
                    }

                    val storageEntry = runtime.metadata.automationTime().storage("AccountTasks")
                    val entries = storageEntry.entriesRaw(accountId)
                    val storagePrefix = storageEntry.storageKey(runtime, accountId)

                    storageCache.insertPrefixEntries(entries, prefix = storagePrefix, chainId = chain.id)
                }
            }.noSideAffects()
    }

    private suspend fun StorageQueryContext.awaitSameAccountValue(accountId: AccountId, expected: String?) {
        suspend fun checkSame(): Boolean {
            val actual = runtime.metadata.system().storage("Account").queryRaw(accountId)

            return actual == expected
        }

        // no delay at the first try
        if (checkSame()) {
            return
        }

        do {
            delay(500)
        } while (!checkSame())
    }
}
