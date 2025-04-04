package io.novafoundation.nova.feature_xcm_api.runtimeApi

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResultError
import io.novafoundation.nova.common.data.network.runtime.binding.toResult

fun <T> Result<ScaleResult<T, *>>.getInnerSuccessOrThrow(errorLogTag: String?): T {
    return getOrThrow()
        .toResult()
        .onFailure {
            Log.e(errorLogTag, "Xcm api call failed: ${(it as ScaleResultError).content}")
        }.getOrThrow()
}
