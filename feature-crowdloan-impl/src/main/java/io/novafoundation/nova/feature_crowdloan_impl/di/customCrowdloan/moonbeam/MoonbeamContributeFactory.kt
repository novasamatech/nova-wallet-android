package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.moonbeam

import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamPrivateSignatureProvider
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamStartFlowInterceptor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main.ConfirmContributeMoonbeamCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main.SelectContributeMoonbeamCustomization

class MoonbeamContributeFactory(
    override val submitter: CustomContributeSubmitter,
    override val startFlowInterceptor: MoonbeamStartFlowInterceptor,
    override val privateCrowdloanSignatureProvider: MoonbeamPrivateSignatureProvider,
    override val selectContributeCustomization: SelectContributeMoonbeamCustomization,
    override val confirmContributeCustomization: ConfirmContributeMoonbeamCustomization,
) : CustomContributeFactory {

    override val flowType: String = "Moonbeam"
}
