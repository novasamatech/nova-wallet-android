package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters

import android.util.Log
import io.novafoundation.nova.common.data.network.StorageSubscriptionBuilder
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.PaymentUpdaterFactory
import io.novafoundation.nova.runtime.ext.pairWithAssets
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.subscribeUsing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class BalancesUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val paymentUpdaterFactory: PaymentUpdaterFactory,
    private val accountUpdateScope: AccountUpdateScope,
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return accountUpdateScope.invalidationFlow().flatMapLatest {
            val chains = chainRegistry.currentChains.first()

            val mergedFlow = chains.flatMap { chain -> chain.pairWithAssets() }
                .map { (chain, chainAsset) ->
                    flow {
                        val updater = paymentUpdaterFactory.create(chain, chainAsset)
                        val socket = chainRegistry.getSocket(chain.id)

                        val subscriptionBuilder = StorageSubscriptionBuilder.create(socket)

                        kotlin.runCatching {
                            updater.listenForUpdates(subscriptionBuilder)
                                .catch { logError(chain, chainAsset, it) }
                        }.onSuccess { updaterFlow ->
                            val cancellable = socket.subscribeUsing(subscriptionBuilder.build())

                            updaterFlow.onCompletion { cancellable.cancel() }

                            emitAll(updaterFlow)
                        }.onFailure {
                            logError(chain, chainAsset, it)
                        }
                    }
                }.merge()

            mergedFlow
        }.flowOn(Dispatchers.Default)
    }

    private fun logError(chain: Chain, chainAsset: Chain.Asset, error: Throwable) {
        Log.e(LOG_TAG, "Failed to subscribe to balances in ${chain.name}.${chainAsset.name}: ${error.message}")
    }
}
