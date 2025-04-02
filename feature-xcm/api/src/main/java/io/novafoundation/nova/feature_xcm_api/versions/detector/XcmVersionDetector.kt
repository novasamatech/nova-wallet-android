package io.novafoundation.nova.feature_xcm_api.versions.detector

import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.definitions.types.RuntimeType

interface XcmVersionDetector {

    suspend fun lowestPresentMultiLocationVersion(chainId: ChainId): XcmVersion?

    suspend fun lowestPresentMultiAssetsVersion(chainId: ChainId): XcmVersion?

    suspend fun lowestPresentMultiAssetVersion(chainId: ChainId): XcmVersion?

    suspend fun detectMultiLocationVersion(chainId: ChainId, multiLocationType: RuntimeType<*, *>?): XcmVersion?
}
