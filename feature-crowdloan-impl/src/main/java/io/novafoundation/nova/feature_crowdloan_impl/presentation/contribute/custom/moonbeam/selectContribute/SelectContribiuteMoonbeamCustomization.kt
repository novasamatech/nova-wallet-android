package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.selectContribute

import android.content.Context
import android.view.View
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
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.LabeledTextView
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.MainFlowCustomization
import kotlinx.android.synthetic.main.fragment_contribute.view.crowdloanContributeDescription
import kotlinx.android.synthetic.main.fragment_contribute.view.crowdloanContributeScrollableContent
import kotlinx.android.synthetic.main.fragment_contribute_confirm.view.confirmContributeAmount
import kotlinx.android.synthetic.main.fragment_contribute_confirm.view.confirmContributeInjectionParent
import kotlinx.coroutines.CoroutineScope

abstract class MainFlowMoonbeamCustomization(
    private val viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
    private val imageLoader: ImageLoader,
) : MainFlowCustomization {

    protected abstract fun getInjectionContainer(into: ViewGroup): ViewGroup

    protected abstract fun getAnchor(into: ViewGroup): View

    protected abstract fun createTitleView(context: Context): TextView

    override fun injectViews(into: ViewGroup, state: MainFlowCustomization.ViewState, scope: LifecycleCoroutineScope) {
        require(state is MoonbeamMainFlowCustomViewState)

        with(into) {
            val title = createTitleView(context)

            val rewardDestinationView = LabeledTextView(context).apply {
                setActionIcon(null)

                layoutParams = injectionLayoutParams(context, topMarginDp = 10)
            }

            getInjectionContainer(into).addAfter(
                anchor = getAnchor(into),
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

    protected fun injectionLayoutParams(
        context: Context,
        topMarginDp: Int,
    ): LinearLayout.LayoutParams {
        val horizontalMargin = 16.dp(context)

        return LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(horizontalMargin, topMarginDp.dp(context), horizontalMargin, 0)
        }
    }

    override fun createViewState(coroutineScope: CoroutineScope, parachainMetadata: ParachainMetadata?): MoonbeamMainFlowCustomViewState {
        return viewStateFactory.create(coroutineScope, parachainMetadata!!)
    }
}

class SelectContributeMoonbeamCustomization(
    viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
    imageLoader: ImageLoader,
) : MainFlowMoonbeamCustomization(viewStateFactory, imageLoader) {

    override fun getInjectionContainer(into: ViewGroup): ViewGroup = into.crowdloanContributeScrollableContent

    override fun getAnchor(into: ViewGroup): View = into.crowdloanContributeDescription

    override fun createTitleView(context: Context): TextView {
        return TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_Header4).apply {
            layoutParams = injectionLayoutParams(context, topMarginDp = 22)
        }
    }
}

class ConfirmContributeMoonbeamCustomization(
    viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
    imageLoader: ImageLoader,
) : MainFlowMoonbeamCustomization(viewStateFactory, imageLoader) {

    override fun getInjectionContainer(into: ViewGroup): ViewGroup = into.confirmContributeInjectionParent

    override fun getAnchor(into: ViewGroup): View = into.confirmContributeAmount

    override fun createTitleView(context: Context): TextView {
        return TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_Body1).apply {
            layoutParams = injectionLayoutParams(context, topMarginDp = 0)

            setTextColorRes(R.color.black1)
        }
    }
}
