package io.novafoundation.nova.feature_account_api.presenatation.chain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatListPreview
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.R

class ChainListOverview(
    val icon: String?,
    val value: String,
    val label: String,
    val hasMoreElements: Boolean,
)

fun ResourceManager.formatChainListOverview(chains: List<ChainUi>): ChainListOverview {
    return ChainListOverview(
        icon = chains.singleOrNull()?.icon,
        value = formatListPreview(chains.map(ChainUi::name)),
        hasMoreElements = chains.size > 1,
        label = getQuantityString(R.plurals.common_networks_plural, chains.size)
    )
}

fun TableCellView.showChainsOverview(chainListOverview: ChainListOverview) {
    setTitle(chainListOverview.label)
    showValue(chainListOverview.value)

    loadImage(chainListOverview.icon, roundedCornersDp = null)

    if (chainListOverview.hasMoreElements) {
        isClickable = true
        setPrimaryValueEndIcon(R.drawable.ic_info)
    } else {
        isClickable = false
        setPrimaryValueEndIcon(null)
    }
}
