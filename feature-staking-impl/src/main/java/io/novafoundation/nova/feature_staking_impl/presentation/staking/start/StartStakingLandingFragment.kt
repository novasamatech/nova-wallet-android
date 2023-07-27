package io.novafoundation.nova.feature_staking_impl.presentation.staking.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.dialog
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
        startStakingLandingToolbar.setHomeButtonListener { viewModel.back() }
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

        viewModel.modelFlow.observe {
            val isLoaded = it.isLoaded()

            headerAdapter.show(isLoaded)
            footerAdapter.show(isLoaded)
            shimmeringAdapter.show(it.isLoading())
            startStakingLandingButton.setProgress(it.isLoading())

            when (it) {
                is ExtendedLoadingState.Loaded<StartStakingInfoModel> -> {
                    headerAdapter.setTitle(it.data.title)
                    conditionsAdapter.submitList(it.data.conditions)
                    footerAdapter.setMoreInformationText(it.data.moreInfo)
                }
                is ExtendedLoadingState.Error -> {
                    dialog(providedContext) {
                        setTitle(providedContext.getString(io.novafoundation.nova.common.R.string.common_error_general_title))
                        it.exception.message?.let { setMessage(it) }
                        setPositiveButton(io.novafoundation.nova.common.R.string.common_ok) { _, _ -> viewModel.back() }
                    }
                }
                else -> {}
            }
        }

        viewModel.availableBalanceTextFlow.observe {
            startStakingLandingAvailableBalance.text = it.dataOrNull
        }
    }

    override fun onTermsOfUseClicked() {
        viewModel.termsOfUseClicked()
    }
}
