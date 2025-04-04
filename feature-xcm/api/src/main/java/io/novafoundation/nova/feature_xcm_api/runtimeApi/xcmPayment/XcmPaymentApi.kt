package io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment

import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.runtimeApi.xcmPayment.model.QueryXcmWeightErr
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface XcmPaymentApi {

    suspend fun queryXcmWeight(
        chainId: ChainId,
        xcm: VersionedXcmMessage,
    ): Result<ScaleResult<WeightV2, QueryXcmWeightErr>>

    suspend fun isSupported(chainId: ChainId): Boolean
}
