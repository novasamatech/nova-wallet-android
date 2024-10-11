package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.observeRewardDestinationChooser

class SelectRewardDestinationFragment : BaseFragment<SelectRewardDestinationViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_select_reward_destination, container, false)
    }

    override fun initViews() {
        selectRewardDestinationContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        selectRewardDestinationToolbar.setHomeButtonListener { viewModel.backClicked() }

        selectRewardDestinationContinue.prepareForProgress(viewLifecycleOwner)
        selectRewardDestinationContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectRewardDestinationFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectRewardDestinationViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        observeBrowserEvents(viewModel)
        observeRewardDestinationChooser(viewModel, selectRewardDestinationChooser)

        viewModel.showNextProgress.observe(selectRewardDestinationContinue::setProgressState)

        viewModel.feeLiveData.observe(selectRewardDestinationFee::setFeeStatus)

        viewModel.continueAvailable.observe {
            val state = if (it) ButtonState.NORMAL else ButtonState.DISABLED

            selectRewardDestinationContinue.setState(state)
        }
    }
}
