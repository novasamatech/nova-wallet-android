package io.novafoundation.nova.feature_assets.presentation.swap.asset

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

internal data class SwapTokenHeader(val title: String)

internal data class SwapTokenRow(
    val id: FullChainAssetId,
    val icon: Icon,
    val title: String,
    val network: String,
    val amount: AmountModel?,
    val networkIcon: Icon,
    val selected: Boolean
)

internal data class SwapNetworkFilter(val chainId: ChainId, val title: String, val icon: Icon, val selected: Boolean)

internal sealed interface SwapNetworkChip {
    data class All(val title: String, val selected: Boolean) : SwapNetworkChip
    data class Network(val network: SwapNetworkFilter) : SwapNetworkChip
    data class More(val title: String) : SwapNetworkChip
}

internal data class SwapTokenSelectionModel(
    val title: String,
    val items: List<Any> = emptyList(),
    val networks: List<SwapNetworkFilter> = emptyList(),
    val chips: List<SwapNetworkChip> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val selecting: Boolean = false
)
