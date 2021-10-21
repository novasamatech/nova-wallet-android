package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan

import android.content.Context
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeView
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import kotlinx.coroutines.CoroutineScope

interface CustomContributeFactory {

    val flowType: String

    fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): CustomContributeViewState

    fun createView(context: Context): CustomContributeView

    val submitter: CustomContributeSubmitter
}

fun CustomContributeFactory.supports(otherFlowType: String): Boolean {
    return otherFlowType == flowType
}
