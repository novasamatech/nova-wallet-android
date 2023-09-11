package io.novafoundation.nova.runtime.state

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

typealias AnySelectedAssetOptionSharedState = SelectedAssetOptionSharedState<*>

interface SelectedAssetOptionSharedState<out A> : ChainIdHolder {

    val selectedOption: Flow<SupportedAssetOption<A>>

    override suspend fun chainId(): String = chain().id

    data class SupportedAssetOption<out A>(
        val assetWithChain: ChainWithAsset,
        val additional: A
    )
}

val SelectedAssetOptionSharedState<*>.assetWithChain: Flow<ChainWithAsset>
    get() = selectedOption.map { it.assetWithChain }

fun SelectedAssetOptionSharedState<*>.selectedChainFlow() = selectedOption
    .map { it.assetWithChain.chain }
    .distinctUntilChanged()

fun SelectedAssetOptionSharedState<*>.selectedAssetFlow() = selectedOption
    .map { it.assetWithChain.asset }

suspend fun SelectedAssetOptionSharedState<*>.chain() = assetWithChain.first().chain

suspend fun SelectedAssetOptionSharedState<*>.chainAsset() = assetWithChain.first().asset

suspend fun SelectedAssetOptionSharedState<*>.chainAndAsset() = assetWithChain.first()

suspend fun <A> SelectedAssetOptionSharedState<A>.selectedOption(): SelectedAssetOptionSharedState.SupportedAssetOption<A> {
    return selectedOption.first()
}
