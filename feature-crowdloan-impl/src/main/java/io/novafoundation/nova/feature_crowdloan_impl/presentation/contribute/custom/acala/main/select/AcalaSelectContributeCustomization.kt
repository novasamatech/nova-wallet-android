package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.select

import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CrowdloanMainFlowFeatures
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.SelectContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.base.AcalaMainFlowCustomization

private const val LEARN_TYPES_LINK = "https://wiki.acala.network/acala/acala-crowdloan/crowdloan-event#3.2-ways-to-participate"

class AcalaSelectContributeCustomization :
    AcalaMainFlowCustomization<SelectContributeCustomization.ViewState>(),
    SelectContributeCustomization {

    override fun injectViews(
        into: ViewGroup,
        state: SelectContributeCustomization.ViewState,
        scope: LifecycleCoroutineScope,
    ) {
        require(state is AcalaSelectContributeViewState)

        val container = into.crowdloanContributeScrollableContent

        val typeSelector = container.inflateChild(R.layout.view_acala_contribution_type) as RadioGroup
        typeSelector.bindTo(state.selectedContributionTypeIdFlow, scope)

        val learnMoreText = container.inflateChild(R.layout.view_acala_learn_contributions) as TextView
        learnMoreText.setOnClickListener { state.learnContributionTypesClicked() }

        container.addAfter(
            anchor = container.crowdloanContributeTitle,
            newViews = listOf(typeSelector, learnMoreText)
        )
    }

    override fun createViewState(
        features: CrowdloanMainFlowFeatures,
        parachainMetadata: ParachainMetadata,
    ): SelectContributeCustomization.ViewState {
        return AcalaSelectContributeViewState(
            browserable = features.browserable,
            scope = features.coroutineScope,
            acalaContributionsInfoLink = LEARN_TYPES_LINK
        )
    }
}
