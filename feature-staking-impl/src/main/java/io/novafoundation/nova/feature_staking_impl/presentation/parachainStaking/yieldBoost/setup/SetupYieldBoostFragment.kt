package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.scrollOnFocusTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.view.showRewardEstimation
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostCollator
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostContainer
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostContinue
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostFrequency
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostOakLogo
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostOff
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostOn
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostScrollArea
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostThreshold
import kotlinx.android.synthetic.main.fragment_setup_yield_boost.setupYieldBoostToolbar

class SetupYieldBoostFragment : BaseFragment<SetupYieldBoostViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_setup_yield_boost, container, false)
    }

    override fun initViews() {
        setupYieldBoostToolbar.applyStatusBarInsets()

        setupYieldBoostToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        setupYieldBoostContinue.prepareForProgress(viewLifecycleOwner)
        setupYieldBoostContinue.setOnClickListener { viewModel.nextClicked() }

        setupYieldBoostCollator.setOnClickListener { viewModel.selectCollatorClicked() }

        setupYieldBoostOn.setOnClickListener { viewModel.yieldBoostStateChanged(yieldBoostOn = true) }
        setupYieldBoostOff.setOnClickListener { viewModel.yieldBoostStateChanged(yieldBoostOn = false) }

        setupYieldBoostContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        setupYieldBoostScrollArea.scrollOnFocusTo(setupYieldBoostThreshold)

        setYieldViewsVisible(false)
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setupYieldBoostComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SetupYieldBoostViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.boostThresholdChooserMixin, setupYieldBoostThreshold)

        viewModel.selectedCollatorModel.observe {
            setupYieldBoostCollator.setSelectedCollator(it)
        }

        viewModel.chooseCollatorAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                stakedCollatorSelected = { action.onSuccess(it) },
                onCancel = action.onCancel,
                newStakeTargetClicked = null
            ).show()
        }

        viewModel.configurationUi.observe { state ->
            setYieldViewsVisible(state is YieldBoostStateModel.On)

            setupYieldBoostOn.setChecked(state is YieldBoostStateModel.On)
            setupYieldBoostOff.setChecked(state is YieldBoostStateModel.Off)

            if (state is YieldBoostStateModel.On) {
                setupYieldBoostFrequency.text = state.frequencyTitle
            }
        }

        viewModel.buttonState.observe(setupYieldBoostContinue::setState)

        viewModel.rewardsWithYieldBoost.observe(setupYieldBoostOn::showRewardEstimation)
        viewModel.rewardsWithoutYieldBoost.observe(setupYieldBoostOff::showRewardEstimation)
    }

    private fun setYieldViewsVisible(visible: Boolean) {
        listOf(
            setupYieldBoostFrequency,
            setupYieldBoostThreshold,
            setupYieldBoostOakLogo
        ).onEach { it.isVisible =  visible }
    }
}
