package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class StartChangeValidatorsFragment : BaseFragment<StartChangeValidatorsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_start_change_validators, container, false)
    }

    override fun initViews() {
        startChangeValidatorsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        startChangeValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        startChangeValidatorsRecommended.setupAction(viewLifecycleOwner) { viewModel.goToRecommendedClicked() }
        startChangeValidatorsRecommended.setOnLearnMoreClickedListener { viewModel.recommendedLearnMoreClicked() }

        startChangeValidatorsCustom.background = getRoundedCornerDrawable(R.color.block_background).withRippleMask()
        startChangeValidatorsCustom.setOnClickListener { viewModel.goToCustomClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startChangeValidatorsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartChangeValidatorsViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.validatorsLoading.observe { loading ->
            startChangeValidatorsRecommended.action.setProgressState(loading)
            startChangeValidatorsCustom.setInProgress(loading)
        }

        viewModel.customValidatorsTexts.observe {
            startChangeValidatorsToolbar.setTitle(it.toolbarTitle)
            startChangeValidatorsCustom.title.text = it.selectManuallyTitle
            startChangeValidatorsCustom.setBadgeText(it.selectManuallyBadge)
        }
    }
}
