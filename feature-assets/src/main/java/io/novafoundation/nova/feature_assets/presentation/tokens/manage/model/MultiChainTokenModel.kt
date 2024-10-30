package io.novafoundation.nova.feature_assets.presentation.tokens.manage.model

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatListPreview
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.manage.MultiChainToken

data class MultiChainTokenModel(
    val header: HeaderModel,
    val enabled: Boolean,
    val switchable: Boolean
) {

    class HeaderModel(
        val icon: Icon,
        val symbol: String,
        val networks: String,
    )
}

class MultiChainTokenMapper(
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager
) {

    fun mapHeaderToUi(multiChainToken: MultiChainToken): MultiChainTokenModel.HeaderModel {
        return MultiChainTokenModel.HeaderModel(
            icon = assetIconProvider.getAssetIcon(multiChainToken.icon),
            symbol = multiChainToken.symbol,
            networks = constructNetworksSubtitle(multiChainToken)
        )
    }

    private fun constructNetworksSubtitle(multiChainToken: MultiChainToken): String {
        val enabledInstances = multiChainToken.instances
            .filter { it.isEnabled }
            .map { it.chain.name }

        return if (enabledInstances.size == multiChainToken.instances.size) {
            resourceManager.getString(R.string.assets_manage_tokens_all_networks)
        } else {
            resourceManager.formatListPreview(enabledInstances, zeroLabel = R.string.common_disabled)
        }
    }
}
