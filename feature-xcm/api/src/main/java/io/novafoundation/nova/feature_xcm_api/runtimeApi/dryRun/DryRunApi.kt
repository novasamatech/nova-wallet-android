package io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun

import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.CallDryRunEffects
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.DryRunEffectsResultErr
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.XcmDryRunEffects
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcmLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface DryRunApi {

    suspend fun dryRunXcm(
        originLocation: VersionedXcmLocation,
        xcm: VersionedRawXcmMessage,
        chainId: ChainId
    ): Result<ScaleResult<XcmDryRunEffects, DryRunEffectsResultErr>>

    suspend fun dryRunCall(
        originCaller: OriginCaller,
        call: GenericCall.Instance,
        resultsXcmVersion: XcmVersion,
        chainId: ChainId
    ): Result<ScaleResult<CallDryRunEffects, DryRunEffectsResultErr>>
}
