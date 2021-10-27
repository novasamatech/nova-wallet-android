package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload

interface StartFlowInterceptor {

    suspend fun startFlow(payload: ContributePayload)
}
