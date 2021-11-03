package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan

import android.content.Context
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.PrivateCrowdloanSignatureProvider
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.ConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeView
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.SelectContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.StartFlowInterceptor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import kotlinx.coroutines.CoroutineScope

interface CustomContributeFactory {

    val flowType: String

    val privateCrowdloanSignatureProvider: PrivateCrowdloanSignatureProvider?
        get() = null

    val submitter: CustomContributeSubmitter

    val startFlowInterceptor: StartFlowInterceptor?
        get() = null

    val extraBonusFlow: ExtraBonusFlow?
        get() = null

    val selectContributeCustomization: SelectContributeCustomization?
        get() = null

    val confirmContributeCustomization: ConfirmContributeCustomization?
        get() = null
}

interface ExtraBonusFlow {

    fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): CustomContributeViewState

    fun createView(context: Context): CustomContributeView
}

fun CustomContributeFactory.supports(otherFlowType: String): Boolean {
    return otherFlowType == flowType
}
