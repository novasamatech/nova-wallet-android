package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class MultiLocationConverterFactory(
    private val chainRegistry: ChainRegistry,
    private val xcmVersionDetector: XcmVersionDetector,
) {

    fun defaultAsync(chain: Chain, coroutineScope: CoroutineScope): MultiLocationConverter {
        val runtimeAsync = coroutineScope.async { chainRegistry.getRuntime(chain.id) }
        val runtimeSource = RuntimeSource.Async(runtimeAsync)

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtimeSource),
            ForeignAssetsLocationConverter(chain, runtimeSource, xcmVersionDetector)
        )
    }

    suspend fun defaultSync(chain: Chain): MultiLocationConverter {
        val runtimeAsync = chainRegistry.getRuntime(chain.id)
        val runtimeSource = RuntimeSource.Sync(runtimeAsync)

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtimeSource),
            ForeignAssetsLocationConverter(chain, runtimeSource, xcmVersionDetector)
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
