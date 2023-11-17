package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class MultiLocationConverterFactory(private val chainRegistry: ChainRegistry) {

    fun default(chain: Chain, coroutineScope: CoroutineScope): MultiLocationConverter {
        val runtimeAsync = coroutineScope.async { chainRegistry.getRuntime(chain.id) }

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtimeAsync),
            ForeignAssetsLocationConverter(chain, runtimeAsync)
        )
    }
}
