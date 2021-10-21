package io.novafoundation.nova.runtime.state

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private const val DELIMITER = ":"

abstract class SingleAssetSharedState(
    private val preferencesKey: String,
    private val chainRegistry: ChainRegistry,
    private val filter: (Chain, Chain.Asset) -> Boolean,
    private val preferences: Preferences
) : ChainIdHolder {

    data class AssetWithChain(
        val chain: Chain,
        val asset: Chain.Asset,
    )

    val assetWithChain: Flow<AssetWithChain> = preferences.stringFlow(
        field = preferencesKey,
        initialValueProducer = {
            val defaultAsset = availableToSelect().first()

            encode(defaultAsset.chainId, defaultAsset.id)
        }
    )
        .filterNotNull()
        .map { encoded ->
            val (chainId, chainAssetId) = decode(encoded)

            val chain = chainRegistry.getChain(chainId)
            val chainAsset = chain.assetsById.getValue(chainAssetId)

            AssetWithChain(chain, chainAsset)
        }
        .inBackground()
        .shareIn(GlobalScope, started = SharingStarted.Eagerly, replay = 1)

    suspend fun availableToSelect(): List<Chain.Asset> {
        val allChains = chainRegistry.currentChains.first()

        return allChains.map { chain ->
            chain.assets.filter { chainAsset ->
                filter(chain, chainAsset)
            }
        }.flatten()
    }

    fun update(chainId: ChainId, chainAssetId: Int) {
        preferences.putString(preferencesKey, encode(chainId, chainAssetId))
    }

    override suspend fun chainId(): String {
        return assetWithChain.first().chain.id
    }

    private fun encode(chainId: ChainId, chainAssetId: Int): String {
        return "$chainId$DELIMITER$chainAssetId"
    }

    private fun decode(value: String): Pair<ChainId, Int> {
        val (chainId, chainAssetRaw) = value.split(DELIMITER)

        return chainId to chainAssetRaw.toInt()
    }
}

fun SingleAssetSharedState.selectedChainFlow() = assetWithChain
    .map { it.chain }
    .distinctUntilChanged()

suspend fun SingleAssetSharedState.chain() = assetWithChain.first().chain

suspend fun SingleAssetSharedState.chainAsset() = assetWithChain.first().asset

suspend fun SingleAssetSharedState.chainAndAsset() = assetWithChain.first()

fun SingleAssetSharedState.selectedAssetFlow() = assetWithChain
    .map { it.asset }
