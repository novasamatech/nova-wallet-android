package io.novafoundation.nova.runtime.network.updaters

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.assetWithChain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlin.coroutines.coroutineContext

abstract class SingleChainUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : UpdateSystem {

    abstract fun getUpdaters(chain: Chain, chainAsset: Chain.Asset): List<Updater>

    override fun start(): Flow<Updater.SideEffect> = singleAssetSharedState.assetWithChain.flatMapLatest { (chain, chainAsset) ->
        val runtimeMetadata = chainRegistry.getRuntime(chain.id).metadata

        val logTag = this@SingleChainUpdateSystem.LOG_TAG
        val selfName = this@SingleChainUpdateSystem::class.java.simpleName

        val updaters = getUpdaters(chain, chainAsset)

        val scopeFlows = updaters.groupBy(Updater::scope).map { (scope, scopeUpdaters) ->
            scope.invalidationFlow().flatMapLatest {
                val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)

                val updatersFlow = scopeUpdaters
                    .filter { it.requiredModules.all(runtimeMetadata::hasModule) }
                    .map { updater ->
                        updater.listenForUpdates(subscriptionBuilder)
                            .catch { Log.e(logTag, "Failed to start $selfName for ${chain.name}: ${it.message}") }
                            .flowOn(Dispatchers.Default)
                    }

                if (updatersFlow.isNotEmpty()) {
                    subscriptionBuilder.subscribe(coroutineContext)

                    updatersFlow.merge()
                } else {
                    emptyFlow()
                }
            }
        }

        scopeFlows.merge()
    }.flowOn(Dispatchers.Default)
}

class ConstantSingleChainUpdateSystem(
    private val updaters: List<Updater>,
    chainRegistry: ChainRegistry,
    singleAssetSharedState: SingleAssetSharedState,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : SingleChainUpdateSystem(chainRegistry, singleAssetSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(chain: Chain, chainAsset: Chain.Asset): List<Updater> {
        return updaters
    }
}
