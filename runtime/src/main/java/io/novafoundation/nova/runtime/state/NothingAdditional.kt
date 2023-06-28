package io.novafoundation.nova.runtime.state

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

object NothingAdditional : SelectableAssetAdditionalData {

    override val identifier: String = "Nothing"

    override fun format(resourceManager: ResourceManager): String? {
        return null
    }
}

fun uniqueOption(valid: (Chain, Chain.Asset) -> Boolean): SupportedOptionsResolver<NothingAdditional> {
    return { chain, asset ->
        if (valid(chain, asset)) listOf(NothingAdditional) else emptyList()
    }
}
