package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.confirm

import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.ContributionType
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.SelectContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.AcalaCustomizationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

private val CONTRIBUTION_TYPE_BY_ID = mapOf(
    R.id.acalaContributionTypeDirect to ContributionType.DIRECT,
    R.id.acalaContributionTypeLiquid to ContributionType.LIQUID
)

private val DEFAULT_CONTRIBUTION_TYPE_ID = R.id.acalaContributionTypeDirect

class AcalaSelectContributeViewState(
    private val acalaContributionsInfoLink: String,
    private val browserable: Browserable.Presentation,
    scope: CoroutineScope,
) : SelectContributeCustomization.ViewState, CoroutineScope by scope {

    val selectedContributionTypeIdFlow = MutableStateFlow(DEFAULT_CONTRIBUTION_TYPE_ID)

    override suspend fun buildCustomPayload(): AcalaCustomizationPayload {
        val selectedContributionTypeId = selectedContributionTypeIdFlow.first()
        val contributionType = CONTRIBUTION_TYPE_BY_ID.getValue(selectedContributionTypeId)

        return AcalaCustomizationPayload(contributionType)
    }

    fun learnContributionTypesClicked() {
        browserable.showBrowser(acalaContributionsInfoLink)
    }
}
