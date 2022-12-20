package io.novafoundation.nova.feature_assets.presentation.tokens.manage.model

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.manage.MultiChainToken

data class MultiChainTokenModel(
    val header: HeaderModel,
    val enabled: Boolean,
    val switchable: Boolean
) {

    class HeaderModel(
        val icon: String?,
        val symbol: String,
        val networks: String,
    )
}

class MultiChainTokenMapper(
    private val resourceManager: ResourceManager
) {

    fun mapHeaderToUi(multiChainToken: MultiChainToken): MultiChainTokenModel.HeaderModel {
        return MultiChainTokenModel.HeaderModel(
            icon = multiChainToken.icon,
            symbol = multiChainToken.symbol,
            networks = constructNetworksSubtitle(multiChainToken)
        )
    }

    private fun constructNetworksSubtitle(multiChainToken: MultiChainToken): String {
        val enabledInstances = multiChainToken.instances.filter { it.isEnabled }

        return when (enabledInstances.size) {
            0 -> resourceManager.getString(R.string.common_disabled)

            multiChainToken.instances.size -> resourceManager.getString(R.string.assets_manage_tokens_all_networks)

            1 -> enabledInstances.single().chain.name

            else -> {
                val firstChain = enabledInstances.first()
                val othersCount = enabledInstances.size - 1

                resourceManager.getString(
                    R.string.assets_manage_tokens_partial_networks,
                    firstChain.chain.name,
                    othersCount.format()
                )
            }
        }
    }
}
