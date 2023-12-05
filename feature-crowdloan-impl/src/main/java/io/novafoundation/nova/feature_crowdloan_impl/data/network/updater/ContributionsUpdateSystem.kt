package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.transformLatestDiffed
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.isFullSync
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest

class RealContributionsUpdateSystemFactory(
    private val chainRegistry: ChainRegistry,
    private val contributionsUpdaterFactory: ContributionsUpdaterFactory,
    private val assetBalanceScopeFactory: AssetBalanceScopeFactory,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : ContributionsUpdateSystemFactory {

    override fun create(): UpdateSystem {
        return ContributionsUpdateSystem(
            chainRegistry,
            contributionsUpdaterFactory,
            assetBalanceScopeFactory,
            storageSharedRequestsBuilderFactory
        )
    }
}

class ContributionsUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val contributionsUpdaterFactory: ContributionsUpdaterFactory,
    private val assetBalanceScopeFactory: AssetBalanceScopeFactory,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : UpdateSystem {

    override fun start(): Flow<Updater.SideEffect> {
        return flowOfAll {
            chainRegistry.currentChains.mapLatest { chains ->
                chains.filter { it.connectionState.isFullSync && it.hasCrowdloans }
            }.transformLatestDiffed {
                emitAll(run(it))
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun run(chain: Chain): Flow<Updater.SideEffect> {
        return flowOfAll {
            // we do not start subscription builder since it is not needed for contributions
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)
            val invalidationScope = assetBalanceScopeFactory.create(chain, chain.utilityAsset)
            val updater = contributionsUpdaterFactory.create(chain, invalidationScope)

            invalidationScope.invalidationFlow().transformLatest {
                kotlin.runCatching {
                    updater.listenForUpdates(subscriptionBuilder, it)
                        .catch { logError(chain, it) }
                }.onSuccess { updaterFlow ->
                    emitAll(updaterFlow)
                }
            }
        }.catch { logError(chain, it) }
    }

    private fun logError(chain: Chain, exception: Throwable) {
        Log.e(LOG_TAG, "Failed to run contributions updater for ${chain.name}", exception)
    }
}
