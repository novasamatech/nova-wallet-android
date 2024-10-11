package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.confirm

import android.os.Parcelable
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.observeInLifecycle
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.ConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.AcalaCustomizationPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.base.AcalaMainFlowCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.injectionLayoutParams

import kotlinx.coroutines.CoroutineScope

class AcalaConfirmContributeCustomization(
    private val viewStateFactory: AcalaConfirmContributeViewStateFactory,
) : AcalaMainFlowCustomization<ConfirmContributeCustomization.ViewState>(),
    ConfirmContributeCustomization {

    override fun injectViews(
        into: ViewGroup,
        state: ConfirmContributeCustomization.ViewState,
        scope: LifecycleCoroutineScope,
    ) {
        require(state is AcalaConfirmContributeViewState)

        val container = into.confirmContributeInjectionParent

        val contributionCell = TableCellView(into.context).apply {
            layoutParams = injectionLayoutParams(context, topMarginDp = 0)

            setTitle(R.string.crowdloan_contribution)
        }

        state.contributionTypeFlow.observeInLifecycle(scope) {
            contributionCell.showValue(it)
        }

        container.addAfter(
            anchor = container.confirmContributeAmountBottomMargin,
            newViews = listOf(contributionCell)
        )
    }

    override fun createViewState(
        coroutineScope: CoroutineScope,
        parachainMetadata: ParachainMetadata,
        customPayload: Parcelable?,
    ): ConfirmContributeCustomization.ViewState {
        require(customPayload is AcalaCustomizationPayload)

        return viewStateFactory.create(coroutineScope, customPayload)
    }
}
