package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.moonbeam

import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamStartFlowInterceptor

class MoonbeamContributeFactory(
    override val submitter: CustomContributeSubmitter,
    override val startFlowInterceptor: MoonbeamStartFlowInterceptor,
) : CustomContributeFactory {

    override val flowType: String = "Moonbeam"
}


