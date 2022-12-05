package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.transformLatestDiffed
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlin.coroutines.coroutineContext

class BalancesUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val paymentUpdaterFactory: PaymentUpdaterFactory,
    private val balanceLocksUpdater: BalanceLocksUpdaterFactory,
    private val accountUpdateScope: AccountUpdateScope
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return accountUpdateScope.invalidationFlow().flatMapLatest {
            chainRegistry.currentChains.transformLatestDiffed { chain ->
                val updater = balanceChainUpdaters(chain)
                emitAll(updater)
            }
        }.flowOn(Dispatchers.Default)
    }

    private suspend fun balanceChainUpdaters(chain: Chain): Flow<Updater.SideEffect> {
        return flow {
            val socket = chainRegistry.getSocket(chain.id)
            val subscriptionBuilder = StorageSharedRequestsBuilder.create(socket)

            val updaters: List<Updater> = listOf(paymentUpdaterFactory.create(chain), balanceLocksUpdater.create(chain))
            val sideEffectFlows = updaters.map { updater ->
                try {
                    updater.listenForUpdates(subscriptionBuilder).catch { logError(chain, it) }
                } catch (e: Exception) {
                    emptyFlow()
                }
            }

            subscriptionBuilder.subscribe(coroutineContext)
            val resultFlow = sideEffectFlows.merge()

            emitAll(resultFlow)
        }
    }

    private fun logError(chain: Chain, error: Throwable) {
        Log.e(LOG_TAG, "Failed to subscribe to balances in ${chain.name}: ${error.message}", error)
    }
}
