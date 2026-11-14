package io.novafoundation.nova.feature_assets.presentation.swap.asset

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.swap.SwapTokenSection
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class SwapTokenSelectionFormatter(
    private val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val amountFormatter: AmountFormatter
) {

    internal fun network(chain: Chain, selected: Boolean) = SwapNetworkFilter(
        chain.id,
        chain.name,
        chain.icon?.asUrlIcon() ?: R.drawable.ic_fallback_network_icon.asIcon(),
        selected
    )

    internal fun tokens(
        groups: Map<SwapTokenSection, List<AssetWithNetwork>>,
        oppositeSymbol: String?,
        selected: FullChainAssetId?
    ): List<Any> = groups.toListWithHeaders(
        keyMapper = { section, _ ->
            SwapTokenHeader(
                when (section) {
                    SwapTokenSection.YOUR_TOKENS -> resourceManager.getString(R.string.swap_select_your_tokens)
                    SwapTokenSection.OTHER_TOKENS -> resourceManager.getString(R.string.swap_select_other_tokens)
                    SwapTokenSection.INCOMPATIBLE -> resourceManager.getString(R.string.swap_select_not_swappable, oppositeSymbol.orEmpty())
                }
            )
        },
        valueMapper = { item ->
            val asset = item.asset
            val id = asset.token.configuration.fullId
            SwapTokenRow(
                id = id,
                icon = assetIconProvider.getAssetIconOrFallback(asset.token.configuration.icon),
                title = asset.token.configuration.symbol.value,
                network = item.chain.name,
                amount = if (asset.totalInPlanks.signum() > 0) {
                    amountFormatter.formatAmountToAmountModel(asset.transferable, asset, AmountConfig(includeAssetTicker = false))
                        .let { it.copy(fiat = it.fiat.takeIf { asset.token.coinRate != null }) }
                } else {
                    null
                },
                networkIcon = network(item.chain, false).icon,
                selected = id == selected
            )
        }
    )
}
