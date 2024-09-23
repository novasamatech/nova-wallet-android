package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class MultiLocationConverterFactory(private val chainRegistry: ChainRegistry) {

    fun defaultAsync(chain: Chain, coroutineScope: CoroutineScope): MultiLocationConverter {
        val runtimeAsync = coroutineScope.async { chainRegistry.getRuntime(chain.id) }

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, RuntimeSource.Async(runtimeAsync)),
            ForeignAssetsLocationConverter(chain, runtimeAsync)
        )
    }

    suspend fun resolveLocalAssets(chain: Chain): MultiLocationConverter {
        val runtime = chainRegistry.getRuntime(chain.id)

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, RuntimeSource.Sync(runtime)),
        )
    }
}
