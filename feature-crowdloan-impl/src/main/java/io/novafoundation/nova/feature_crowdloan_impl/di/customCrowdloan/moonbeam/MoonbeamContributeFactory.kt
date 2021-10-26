package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.moonbeam

import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamPrivateSignatureProvider
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamStartFlowInterceptor

class MoonbeamContributeFactory(
    override val submitter: CustomContributeSubmitter,
    override val startFlowInterceptor: MoonbeamStartFlowInterceptor,
    override val privateCrowdloanSignatureProvider: MoonbeamPrivateSignatureProvider,
) : CustomContributeFactory {

    override val flowType: String = "Moonbeam"
}


