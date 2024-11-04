package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import android.os.Bundle
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitableActionFlow
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStartStakingLandingBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload

class StartStakingLandingFragment :
    BaseFragment<StartStakingLandingViewModel, FragmentStartStakingLandingBinding>(),
    StartStakingLandingFooterAdapter.ClickHandler {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: StartStakingLandingPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentStartStakingLandingBinding.inflate(layoutInflater)

    private val headerAdapter = StartStakingLandingHeaderAdapter()
    private val conditionsAdapter = StartStakingLandingAdapter()
    private val footerAdapter = StartStakingLandingFooterAdapter(this)
    private val shimmeringAdapter = CustomPlaceholderAdapter(R.layout.item_start_staking_landing_shimmering)
    private val adapter = ConcatAdapter(shimmeringAdapter, headerAdapter, conditionsAdapter, footerAdapter)

    override fun initViews() {
        binder.startStakingLandingToolbar.applyStatusBarInsets()
        binder.startStakingLandingToolbar.setHomeButtonListener { viewModel.back() }
        binder.startStakingLandingList.adapter = adapter
        binder.startStakingLandingList.itemAnimator = null

        binder.startStakingLandingButton.prepareForProgress(viewLifecycleOwner)
        binder.startStakingLandingButton.setOnClickListener { viewModel.continueClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startStakingLandingComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: StartStakingLandingViewModel) {
        observeBrowserEvents(viewModel)
        observeValidations(viewModel)

        viewModel.isContinueButtonLoading.observe(binder.startStakingLandingButton::setProgressState)

        viewModel.modelFlow.observe {
            val isLoaded = it.isLoaded()

            headerAdapter.show(isLoaded)
            footerAdapter.show(isLoaded)
            shimmeringAdapter.show(it.isLoading())

            when (it) {
                is ExtendedLoadingState.Loaded<StartStakingInfoModel> -> {
                    headerAdapter.setTitle(it.data.title)
                    conditionsAdapter.submitList(it.data.conditions)
                    footerAdapter.setMoreInformationText(it.data.moreInfo)
                    binder.startStakingLandingButton.setButtonColor(it.data.buttonColor)
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
            binder.startStakingLandingAvailableBalance.text = it
        }

        viewModel.acknowledgeStakingStarted.awaitableActionFlow.observeWhenCreated { action ->
            dialog(requireContext()) {
                setTitle(action.payload)

                setPositiveButton(R.string.common_close) { _, _ ->
                    action.onSuccess(Unit)
                }
            }
        }
    }

    override fun onTermsOfUseClicked() {
        viewModel.termsOfUseClicked()
    }
}
