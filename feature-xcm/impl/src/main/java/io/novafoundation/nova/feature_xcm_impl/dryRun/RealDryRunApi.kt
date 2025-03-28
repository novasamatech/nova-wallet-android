package io.novafoundation.nova.feature_xcm_impl.dryRun

import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.feature_xcm_api.dryRun.DryRunApi
import io.novafoundation.nova.feature_xcm_api.dryRun.model.CallDryRunEffects
import io.novafoundation.nova.feature_xcm_api.dryRun.model.DryRunEffectsResultErr
import io.novafoundation.nova.feature_xcm_api.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.dryRun.model.XcmDryRunEffects
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcmLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import javax.inject.Inject

@FeatureScope
class RealDryRunApi @Inject constructor(
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi
) : DryRunApi {

    override suspend fun dryRunXcm(
        originLocation: VersionedXcmLocation,
        xcm: VersionedRawXcmMessage,
        chainId: ChainId
    ): Result<ScaleResult<XcmDryRunEffects, DryRunEffectsResultErr>> {
        return multiChainRuntimeCallsApi.forChain(chainId).dryRunXcm(xcm, originLocation)
    }

    override suspend fun dryRunCall(
        originCaller: OriginCaller,
        call: GenericCall.Instance,
        xcmResultsVersion: XcmVersion,
        chainId: ChainId
    ): Result<ScaleResult<CallDryRunEffects, DryRunEffectsResultErr>> {
        return multiChainRuntimeCallsApi.forChain(chainId).dryRunCall(originCaller, call, xcmResultsVersion)
    }

    private suspend fun RuntimeCallsApi.dryRunXcm(
        xcm: VersionedRawXcmMessage,
        origin: VersionedXcmLocation,
    ): Result<ScaleResult<XcmDryRunEffects, DryRunEffectsResultErr>> {
        return runCatching {
            call(
                section = "DryRunApi",
                method = "dry_run_xcm",
                arguments = mapOf(
                    "origin_location" to origin.toEncodableInstance(),
                    "xcm" to xcm.toEncodableInstance()
                ),
                returnBinding = {
                    runtime.provideContext {
                        ScaleResult.bind(
                            dynamicInstance = it,
                            bindOk = { XcmDryRunEffects.bind(it) },
                            bindError = DryRunEffectsResultErr::bind
                        )
                    }
                }
            )
        }
    }

    private suspend fun RuntimeCallsApi.dryRunCall(
        originCaller: OriginCaller,
        call: GenericCall.Instance,
        xcmResultsVersion: XcmVersion,
        ): Result<ScaleResult<CallDryRunEffects, DryRunEffectsResultErr>> {
        return runCatching {
            call(
                section = "DryRunApi",
                method = "dry_run_call",
                arguments = mapOf(
                    "origin" to originCaller.toEncodableInstance(),
                    "call" to call,
                    "xcm_results_version" to xcmResultsVersion.version.toBigInteger()
                ),
                returnBinding = {
                    runtime.provideContext {
                        ScaleResult.bind(
                            dynamicInstance = it,
                            bindOk = { CallDryRunEffects.bind(it) },
                            bindError = DryRunEffectsResultErr::bind
                        )
                    }
                }
            )
        }
    }
}
