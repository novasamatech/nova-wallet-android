package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.selectContribute

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.observeInLifecycle
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.LabeledTextView
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.MainFlowCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataFromParcel
import kotlinx.android.synthetic.main.fragment_contribute.view.crowdloanContributeDescription
import kotlinx.android.synthetic.main.fragment_contribute.view.crowdloanContributeScrollableContent
import kotlinx.coroutines.CoroutineScope

class SelectContributeMoonbeamCustomization(
    private val viewStateFactory: SelectContributeMoonbeamViewStateFactory,
    private val imageLoader: ImageLoader,
) : MainFlowCustomization {

    override fun injectViews(into: ViewGroup, state: MainFlowCustomization.ViewState, scope: LifecycleCoroutineScope) {
        require(state is SelectContributeMoonbeamViewState)

        with(into) {
            val title = TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_Header4).apply {
                layoutParams = injectionLayoutParams(context, topMarginDp = 22)
            }

            val rewardDestinationView = LabeledTextView(context).apply {
                setActionIcon(null)

                layoutParams = injectionLayoutParams(context, topMarginDp = 10)
            }

            crowdloanContributeScrollableContent.addAfter(
                anchor = crowdloanContributeDescription,
                newViews = listOf(title, rewardDestinationView)
            )

            state.moonbeamRewardDestination.observeInLifecycle(scope) {
                title.text = it.title

                rewardDestinationView.primaryIcon.setVisible(true)
                rewardDestinationView.primaryIcon.load(it.chain.icon, imageLoader)
                rewardDestinationView.setTextIcon(it.addressModel.image)
                rewardDestinationView.setMessage(it.addressModel.address)
                rewardDestinationView.setLabel(it.chain.name)
            }
        }
    }

    private fun injectionLayoutParams(
        context: Context,
        topMarginDp: Int,
    ): LinearLayout.LayoutParams {
        val horizontalMargin = 16.dp(context)

        return LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(horizontalMargin, topMarginDp.dp(context), horizontalMargin, 0)
        }
    }

    override fun createViewState(coroutineScope: CoroutineScope, contributionPayload: ContributePayload): SelectContributeMoonbeamViewState {
        return viewStateFactory.create(coroutineScope, mapParachainMetadataFromParcel(contributionPayload.parachainMetadata!!))
    }
}
