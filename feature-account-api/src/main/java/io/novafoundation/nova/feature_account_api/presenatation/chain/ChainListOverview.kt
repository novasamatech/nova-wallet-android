package io.novafoundation.nova.feature_account_api.presenatation.chain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatListPreview
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.R

class ChainListOverview(
    val icon: String?,
    val label: String,
    val hasMoreElements: Boolean,
)

fun ResourceManager.formatChainListOverview(chains: List<ChainUi>): ChainListOverview {
    return ChainListOverview(
        icon = chains.singleOrNull()?.icon,
        label = formatListPreview(chains.map(ChainUi::name)),
        hasMoreElements = chains.size > 1
    )
}

fun TableCellView.showChainsOverview(chainListOverview: ChainListOverview) {
    showValue(chainListOverview.label)

    loadImage(chainListOverview.icon, roundedCornersDp = null)

    if (chainListOverview.hasMoreElements) {
        isClickable = true
        setPrimaryValueIcon(R.drawable.ic_info_cicrle_filled_16)
    } else {
        isClickable = false
        setPrimaryValueIcon(null)
    }
}
