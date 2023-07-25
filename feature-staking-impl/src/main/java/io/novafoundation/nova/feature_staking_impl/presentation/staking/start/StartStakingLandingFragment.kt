package io.novafoundation.nova.feature_staking_impl.presentation.staking.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_start_staking_landing.startStakingLandingAvailableBalance
import kotlinx.android.synthetic.main.fragment_start_staking_landing.startStakingLandingButton
import kotlinx.android.synthetic.main.fragment_start_staking_landing.startStakingLandingList
import kotlinx.android.synthetic.main.fragment_start_staking_landing.startStakingLandingToolbar

class StartStakingLandingFragment : BaseFragment<StartStakingLandingViewModel>(), StartStakingLandingFooterAdapter.ClickHandler {

    private val headerAdapter = StartStakingLandingHeaderAdapter()
    private val conditionsAdapter = StartStakingLandingAdapter()
    private val footerAdapter = StartStakingLandingFooterAdapter(this)
    private val shimmeringAdapter = CustomPlaceholderAdapter(R.layout.item_start_staking_landing_shimmering)
    private val adapter = ConcatAdapter(shimmeringAdapter, headerAdapter, conditionsAdapter, footerAdapter)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_start_staking_landing, container, false)
    }

    override fun initViews() {
        startStakingLandingToolbar.applyStatusBarInsets()
        startStakingLandingToolbar.setHomeButtonListener { viewModel.backClicked() }
        startStakingLandingList.adapter = adapter
        startStakingLandingList.itemAnimator = null

        startStakingLandingButton.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startStakingLandingComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartStakingLandingViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.isLoadingStateFlow.observe { isLoading ->
            headerAdapter.show(!isLoading)
            footerAdapter.show(!isLoading)
            shimmeringAdapter.show(isLoading)
            startStakingLandingButton.setProgress(isLoading)
        }

        viewModel.titleFlow.observe { title ->
            headerAdapter.setTitle(title)
        }

        viewModel.stakingConditionsUIFlow.observe { items ->
            conditionsAdapter.submitList(items)
        }

        viewModel.moreInfoTextFlow.observe { text ->
            footerAdapter.setMoreInformationText(text)
        }

        viewModel.availableBalanceTextFlow.observe {
            startStakingLandingAvailableBalance.text = it
        }
    }

    override fun onTermsOfUseClicked() {
        viewModel.termsOfUseClicked()
    }
}
