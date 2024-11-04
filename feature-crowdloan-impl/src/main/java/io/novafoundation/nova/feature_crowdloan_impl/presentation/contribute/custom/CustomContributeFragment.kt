package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentCustomContributeBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class CustomContributeFragment : BaseFragment<CustomContributeViewModel, FragmentCustomContributeBinding>() {

    @Inject
    lateinit var contributionManager: CustomContributeManager

    companion object {

        fun getBundle(payload: CustomContributePayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override val binder by viewBinding(FragmentCustomContributeBinding::bind)

    override fun initViews() {
        binder.customContributeContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        binder.customContributeToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.customContributeApply.prepareForProgress(viewLifecycleOwner)
        binder.customContributeApply.setOnClickListener { viewModel.applyClicked() }
    }

    override fun inject() {
        val payload = argument<CustomContributePayload>(KEY_PAYLOAD)

        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .customContributeFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: CustomContributeViewModel) {
        lifecycleScope.launchWhenResumed {
            viewModel.applyButtonState.combine(viewModel.applyingInProgress) { state, inProgress ->
                when {
                    inProgress -> binder.customContributeApply.setState(ButtonState.PROGRESS)
                    state is ApplyActionState.Unavailable -> {
                        binder.customContributeApply.setState(ButtonState.DISABLED)
                        binder.customContributeApply.text = state.reason
                    }

                    state is ApplyActionState.Available -> {
                        binder.customContributeApply.setState(ButtonState.NORMAL)
                        binder.customContributeApply.setText(R.string.common_apply)
                    }
                }
            }.collect()
        }

        viewModel.viewStateFlow.observe { viewState ->
            binder.customFlowContainer.removeAllViews()

            val newView = contributionManager.relevantExtraBonusFlow(viewModel.customFlowType).createView(requireContext())

            binder.customFlowContainer.addView(newView)

            newView.bind(viewState, lifecycleScope)
        }
    }
}
