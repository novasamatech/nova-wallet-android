package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.common.data.network.StorageSubscriptionBuilder
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.subscribeUsing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion

class BalancesUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val paymentUpdaterFactory: PaymentUpdaterFactory,
    private val balanceLocksUpdater: BalanceLocksUpdaterFactory,
    private val accountUpdateScope: AccountUpdateScope
) : UpdateSystem {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun start(): Flow<Updater.SideEffect> {
        return accountUpdateScope.invalidationFlow().flatMapLatest {
            val chains = chainRegistry.currentChains.first()

            val mergedFlow = chains.map { chain ->
                flow {
                    val socket = chainRegistry.getSocket(chain.id)
                    val subscriptionBuilder = StorageSubscriptionBuilder.create(socket)

                    val updaters: List<Updater> = listOf(
                        paymentUpdaterFactory.create(chain),
                        balanceLocksUpdater.create(chain)
                    )

                    kotlin.runCatching {
                        updaters.map { it.listenForUpdates(subscriptionBuilder) }
                            .merge()
                            .catch { logError(chain, it) }
                    }.onSuccess { updatersFlow ->
                        val cancellable = socket.subscribeUsing(subscriptionBuilder.build())

                        updatersFlow.onCompletion { cancellable.cancel() }

                        emitAll(updatersFlow)
                    }.onFailure {
                        logError(chain, it)
                    }
                }
            }.merge()

            mergedFlow
        }.flowOn(Dispatchers.Default)
    }

    private fun logError(chain: Chain, error: Throwable) {
        Log.e(LOG_TAG, "Failed to subscribe to balances in ${chain.name}: ${error.message}", error)
    }
}
