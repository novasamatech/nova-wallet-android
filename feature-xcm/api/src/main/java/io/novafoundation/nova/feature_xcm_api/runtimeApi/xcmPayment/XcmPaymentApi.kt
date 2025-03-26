package io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResultError
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.toResult
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.DryRunEffectsResultErr
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.model.QueryXcmWeightErr
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface XcmPaymentApi {

    suspend fun queryXcmWeight(
        chainId: ChainId,
        xcm: VersionedXcmMessage,
    ): Result<ScaleResult<WeightV2, QueryXcmWeightErr>>
}

fun <T> Result<ScaleResult<T, DryRunEffectsResultErr>>.get(errorLogTag: String?): T {
    return getOrThrow()
        .toResult()
        .onFailure {
            Log.e(errorLogTag, "Dry run failed: ${(it as ScaleResultError).content}")
        }.getOrThrow()
}
