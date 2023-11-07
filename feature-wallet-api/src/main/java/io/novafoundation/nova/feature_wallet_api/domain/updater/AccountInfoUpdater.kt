package io.novafoundation.nova.feature_wallet_api.domain.updater

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.ChainUpdateScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class AccountInfoUpdaterFactory(
    private val storageCache: StorageCache,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) {

    fun create(chainUpdateScope: ChainUpdateScope, sharedState: SelectedAssetOptionSharedState<*>): AccountInfoUpdater {
        return AccountInfoUpdater(
            chainUpdateScope = chainUpdateScope,
            storageCache = storageCache,
            sharedState = sharedState,
            accountRepository = accountRepository,
            chainRegistry = chainRegistry
        )
    }
}

class AccountInfoUpdater(
    chainUpdateScope: ChainUpdateScope,
    storageCache: StorageCache,
    sharedState: SelectedAssetOptionSharedState<*>,
    chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : SingleStorageKeyUpdater<Chain>(chainUpdateScope, sharedState, chainRegistry, storageCache) {

    override val requiredModules: List<String> = listOf(Modules.SYSTEM)

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Chain): String? {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = metaAccount.accountIdIn(scopeValue) ?: return null
        return runtime.metadata.module(Modules.SYSTEM).storage("Account").storageKey(runtime, accountId)
    }
}

