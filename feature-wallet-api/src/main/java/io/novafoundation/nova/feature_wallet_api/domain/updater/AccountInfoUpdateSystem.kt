package io.novafoundation.nova.feature_wallet_api.domain.updater

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.ChainUpdateScope
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class AccountInfoUpdateSystemFactory(
    private val accountInfoUpdaterFactory: AccountInfoUpdaterFactory,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) {
    fun create(chainFlow: Flow<Chain>): AccountInfoUpdateSystem {
        return AccountInfoUpdateSystem(
            ChainUpdateScope(chainFlow),
            accountInfoUpdaterFactory,
            storageSharedRequestsBuilderFactory
        )
    }
}

class AccountInfoUpdateSystem(
    private val chainUpdateScope: ChainUpdateScope,
    private val accountInfoUpdaterFactory: AccountInfoUpdaterFactory,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return chainUpdateScope.invalidationFlow().flatMapLatest {
            val sharedRequestBuilder = storageSharedRequestsBuilderFactory.create(it.id)
            accountInfoUpdaterFactory.create(chainUpdateScope)
                .listenForUpdates(sharedRequestBuilder, it)
        }
    }
}
