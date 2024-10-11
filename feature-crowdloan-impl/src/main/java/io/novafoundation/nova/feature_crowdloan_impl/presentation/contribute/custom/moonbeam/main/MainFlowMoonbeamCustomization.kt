package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import coil.ImageLoader
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.observeInLifecycle
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.LabeledTextView
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.ConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CrowdloanMainFlowFeatures
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.SelectContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.injectionLayoutParams

import kotlinx.coroutines.CoroutineScope

abstract class MainFlowMoonbeamCustomization(
    private val imageLoader: ImageLoader,
) {

    protected fun injectViews(
        state: MoonbeamMainFlowCustomViewState,
        scope: LifecycleCoroutineScope,
        injectionContainer: ViewGroup,
        anchor: View,
        titleView: TextView,
    ) {
        with(injectionContainer) {
            val rewardDestinationView = LabeledTextView(context).apply {
                setActionIcon(null)

                layoutParams = injectionLayoutParams(context, topMarginDp = 10)
            }

            injectionContainer.addAfter(
                anchor = anchor,
                newViews = listOf(titleView, rewardDestinationView)
            )

            state.moonbeamRewardDestination.observeInLifecycle(scope) {
                titleView.text = it.title

                rewardDestinationView.primaryIcon.setVisible(true)
                rewardDestinationView.primaryIcon.loadChainIcon(it.chain.icon, imageLoader)
                rewardDestinationView.setTextIcon(it.addressModel.image)
                rewardDestinationView.setMessage(it.addressModel.address)
                rewardDestinationView.setLabel(it.chain.name)
            }
        }
    }
}

class SelectContributeMoonbeamCustomization(
    private val viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
    imageLoader: ImageLoader,
) : MainFlowMoonbeamCustomization(imageLoader), SelectContributeCustomization {

    override fun injectViews(into: ViewGroup, state: SelectContributeCustomization.ViewState, scope: LifecycleCoroutineScope) {
        require(state is MoonbeamMainFlowCustomViewState)

        injectViews(
            state = state,
            scope = scope,
            injectionContainer = into.crowdloanContributeScrollableContent,
            anchor = into.crowdloanContributeDescription,
            titleView = TextView(into.context, null, 0, R.style.TextAppearance_NovaFoundation_Header4).apply {
                layoutParams = injectionLayoutParams(context, topMarginDp = 22)
            }
        )
    }

    override fun createViewState(features: CrowdloanMainFlowFeatures, parachainMetadata: ParachainMetadata): SelectContributeCustomization.ViewState {
        return viewStateFactory.create(features.coroutineScope, parachainMetadata)
    }

    override fun modifyValidations(validations: Collection<ContributeValidation>): Collection<ContributeValidation> {
        return validations
    }
}

class ConfirmContributeMoonbeamCustomization(
    private val viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
    imageLoader: ImageLoader,
) : MainFlowMoonbeamCustomization(imageLoader), ConfirmContributeCustomization {

    override fun injectViews(into: ViewGroup, state: ConfirmContributeCustomization.ViewState, scope: LifecycleCoroutineScope) {
        require(state is MoonbeamMainFlowCustomViewState)

        injectViews(
            state = state,
            scope = scope,
            injectionContainer = into.confirmContributeInjectionParent,
            anchor = into.confirmContributeAmount,
            titleView = TextView(into.context, null, 0, R.style.TextAppearance_NovaFoundation_Body1).apply {
                layoutParams = injectionLayoutParams(context, topMarginDp = 12)

                setTextColorRes(R.color.text_secondary)
            }
        )
    }

    override fun createViewState(
        coroutineScope: CoroutineScope,
        parachainMetadata: ParachainMetadata,
        customPayload: Parcelable?,
    ): ConfirmContributeCustomization.ViewState {
        return viewStateFactory.create(coroutineScope, parachainMetadata)
    }

    override fun modifyValidations(validations: Collection<ContributeValidation>): Collection<ContributeValidation> {
        return validations
    }
}
