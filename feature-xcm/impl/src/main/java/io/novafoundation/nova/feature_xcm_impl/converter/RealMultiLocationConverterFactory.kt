package io.novafoundation.nova.feature_xcm_impl.converter

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject

@FeatureScope
class RealMultiLocationConverterFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val xcmVersionDetector: XcmVersionDetector,
): MultiLocationConverterFactory {

    override fun defaultAsync(chain: Chain, coroutineScope: CoroutineScope): MultiLocationConverter {
        val runtimeAsync = coroutineScope.async { chainRegistry.getRuntime(chain.id) }
        val runtimeSource = RuntimeSource.Async(runtimeAsync)

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtimeSource),
            ForeignAssetsLocationConverter(chain, runtimeSource, xcmVersionDetector)
        )
    }

    override suspend fun defaultSync(chain: Chain): MultiLocationConverter {
        val runtimeAsync = chainRegistry.getRuntime(chain.id)
        val runtimeSource = RuntimeSource.Sync(runtimeAsync)

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtimeSource),
            ForeignAssetsLocationConverter(chain, runtimeSource, xcmVersionDetector)
        )
    }

    override suspend fun resolveLocalAssets(chain: Chain): MultiLocationConverter {
        val runtime = chainRegistry.getRuntime(chain.id)

        return CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(
                chain,
                RuntimeSource.Sync(runtime)
            ),
        )
    }
}
