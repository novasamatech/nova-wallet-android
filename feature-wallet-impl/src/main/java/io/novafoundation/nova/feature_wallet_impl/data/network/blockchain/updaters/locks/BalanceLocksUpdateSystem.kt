package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.locks

import android.util.Log
import io.novafoundation.nova.common.data.network.StorageSubscriptionBuilder
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdateSystemFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.subscribeUsing
import kotlinx.coroutines.flow.*

class BalanceLocksUpdateSystemFactoryImpl(
    private val chainRegistry: ChainRegistry,
    private val balanceLocksUpdaterFactory: BalanceLocksUpdaterFactory,
) : BalanceLocksUpdateSystemFactory {

    override fun create(chainId: ChainId, chainAssetId: ChainAssetId): UpdateSystem {
        return BalanceLocksUpdateSystem(
            balanceLocksUpdaterFactory.create(chainId, chainAssetId),
            chainRegistry,
            chainId
        )
    }
}

class BalanceLocksUpdateSystem(
    private val updater: Updater,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> = updater.scope.invalidationFlow().flatMapLatest {
        val logTag = this@BalanceLocksUpdateSystem.LOG_TAG
        val selfName = this@BalanceLocksUpdateSystem::class.java.simpleName

        val chain = chainRegistry.getChain(chainId)
        val socket = chainRegistry.getSocket(chainId)
        val subscriptionBuilder = StorageSubscriptionBuilder.create(socket)

        kotlin.runCatching {
            updater.listenForUpdates(subscriptionBuilder)
        }
            .onSuccess {
                val cancelable = socket.subscribeUsing(subscriptionBuilder.build())
                it.onCompletion {
                    cancelable.cancel()
                }
            }.onFailure {
                Log.e(logTag, "Failed to start $selfName for ${chain.name}: ${it.message}")
            }.getOrNull() ?: emptyFlow()
    }
}
