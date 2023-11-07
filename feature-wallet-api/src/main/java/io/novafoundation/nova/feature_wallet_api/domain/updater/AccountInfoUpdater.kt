package io.novafoundation.nova.feature_wallet_api.domain.updater

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.AccountInfoDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.ChainUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.cache.bindAccountInfoOrDefault
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class AccountInfoUpdaterFactory(
    private val accountInfoRepository: AccountInfoRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) {
    fun create(chainUpdateScope: ChainUpdateScope): AccountInfoUpdater {
        return AccountInfoUpdater(
            chainUpdateScope,
            accountInfoRepository,
            accountRepository,
            chainRegistry
        )
    }
}

class AccountInfoUpdater(
    chainUpdateScope: ChainUpdateScope,
    private val accountInfoRepository: AccountInfoRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) : Updater<Chain> { //SingleKeyStorageUpdater

    override val requiredModules: List<String> = listOf(Modules.SYSTEM)

    override val scope: UpdateScope<Chain> = chainUpdateScope

    // Not handle case for ethereum chain
    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder, scopeValue: Chain): Flow<Updater.SideEffect> {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = metaAccount.accountIdIn(scopeValue) ?: return emptyFlow()
        val runtime = chainRegistry.getRuntime(scopeValue.id)
        val storageKey = runtime.metadata.module(Modules.SYSTEM)
            .storage("Account")
            .storageKey(runtime, accountId)

        return storageSubscriptionBuilder.subscribe(storageKey).map {
            val accountInfo = bindAccountInfoOrDefault(it.value, runtime)
            //TODO Save to local storage
            accountInfoRepository.saveAccountInfo(scopeValue, accountInfo)
        }.noSideAffects()
    }
}

