package io.novafoundation.nova.feature_xcm_api.converter

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

interface MultiLocationConverterFactory {

    fun defaultAsync(chain: Chain, coroutineScope: CoroutineScope): MultiLocationConverter

    suspend fun defaultSync(chain: Chain): MultiLocationConverter

    suspend fun resolveLocalAssets(chain: Chain): MultiLocationConverter
}
