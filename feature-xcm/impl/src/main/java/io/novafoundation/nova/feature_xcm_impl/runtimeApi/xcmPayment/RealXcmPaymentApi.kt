package io.novafoundation.nova.feature_xcm_impl.runtimeApi.xcmPayment

import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.XcmPaymentApi
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.model.QueryXcmWeightErr
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import javax.inject.Inject

@FeatureScope
class RealXcmPaymentApi @Inject constructor(
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
) : XcmPaymentApi {

    override suspend fun queryXcmWeight(
        chainId: ChainId,
        xcm: VersionedXcmMessage
    ): Result<ScaleResult<WeightV2, QueryXcmWeightErr>> {
        return multiChainRuntimeCallsApi.forChain(chainId).queryXcmWeight(xcm)
    }

    override suspend fun isSupported(chainId: ChainId): Boolean {
        return multiChainRuntimeCallsApi.forChain(chainId).isSupported("XcmPaymentApi", "query_xcm_weight")
    }

    private suspend fun RuntimeCallsApi.queryXcmWeight(
        xcm: VersionedXcmMessage,
    ): Result<ScaleResult<WeightV2, QueryXcmWeightErr>> {
        return runCatching {
            call(
                section = "XcmPaymentApi",
                method = "query_xcm_weight",
                arguments = mapOf(
                    "message" to xcm.toEncodableInstance()
                ),
                returnBinding = {
                    ScaleResult.bind(
                        dynamicInstance = it,
                        bindOk = ::bindWeightV2,
                        bindError = QueryXcmWeightErr::bind
                    )
                }
            )
        }
    }
}
