package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import android.util.Log
import io.novafoundation.nova.common.data.network.StorageSubscriptionBuilder
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge

class RealContributionsUpdateSystemFactory(
    private val chainRegistry: ChainRegistry,
    private val accountUpdateScope: AccountUpdateScope,
    private val contributionsUpdaterFactory: ContributionsUpdaterFactory,
    private val assetBalanceScopeFactory: AssetBalanceScopeFactory
) : ContributionsUpdateSystemFactory {

    override fun create(): UpdateSystem {
        return ContributionsUpdateSystem(
            chainRegistry,
            accountUpdateScope,
            contributionsUpdaterFactory,
            assetBalanceScopeFactory
        )
    }
}

class ContributionsUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val accountUpdateScope: AccountUpdateScope,
    private val contributionsUpdaterFactory: ContributionsUpdaterFactory,
    private val assetBalanceScopeFactory: AssetBalanceScopeFactory
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return accountUpdateScope.invalidationFlow().flatMapLatest { metaAccount ->
            chainRegistry.currentChains.first()
                .filter { it.hasCrowdloans }
                .map { chain -> run(chain, metaAccount) }
                .merge()
        }.flowOn(Dispatchers.Default)
    }

    private fun run(chain: Chain, metaAccount: MetaAccount): Flow<Updater.SideEffect> {
        return flow {
            val socket = chainRegistry.getSocket(chain.id)
            val subscriptionBuilder = StorageSubscriptionBuilder.create(socket)
            val invalidationScope = assetBalanceScopeFactory.create(chain.utilityAsset, metaAccount)
            val updater = contributionsUpdaterFactory.create(chain, invalidationScope)

            kotlin.runCatching {
                updater.listenForUpdates(subscriptionBuilder)
                    .catch { logError(it) }
            }.onSuccess { updaterFlow ->
                emitAll(updaterFlow)
            }
        }
    }

    private fun logError(exception: Throwable) {
        Log.e(LOG_TAG, "Failed to run contributions updater: ${exception.message}", exception)
    }
}
