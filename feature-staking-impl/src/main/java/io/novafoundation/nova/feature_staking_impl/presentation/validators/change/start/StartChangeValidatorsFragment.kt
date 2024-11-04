package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.start

import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStartChangeValidatorsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class StartChangeValidatorsFragment : BaseFragment<StartChangeValidatorsViewModel, FragmentStartChangeValidatorsBinding>() {

    override fun createBinding() = FragmentStartChangeValidatorsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.startChangeValidatorsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        binder.startChangeValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.startChangeValidatorsRecommended.setupAction(viewLifecycleOwner) { viewModel.goToRecommendedClicked() }
        binder.startChangeValidatorsRecommended.setOnLearnMoreClickedListener { viewModel.recommendedLearnMoreClicked() }

        binder.startChangeValidatorsCustom.background = getRoundedCornerDrawable(R.color.block_background).withRippleMask()
        binder.startChangeValidatorsCustom.setOnClickListener { viewModel.goToCustomClicked() }
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
            binder.startChangeValidatorsRecommended.action.setProgressState(loading)
            binder.startChangeValidatorsCustom.setInProgress(loading)
        }

        viewModel.customValidatorsTexts.observe {
            binder.startChangeValidatorsToolbar.setTitle(it.toolbarTitle)
            binder.startChangeValidatorsCustom.title.text = it.selectManuallyTitle
            binder.startChangeValidatorsCustom.setBadgeText(it.selectManuallyBadge)
        }
    }
}
