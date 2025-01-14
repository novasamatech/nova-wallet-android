package io.novafoundation.nova.feature_xcm_api.converter

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

interface MultiLocationConverterFactory {

    fun defaultAsync(chain: Chain, coroutineScope: CoroutineScope): MultiLocationConverter

    suspend fun defaultSync(chain: Chain): MultiLocationConverter

    suspend fun resolveLocalAssets(chain: Chain): MultiLocationConverter
}


