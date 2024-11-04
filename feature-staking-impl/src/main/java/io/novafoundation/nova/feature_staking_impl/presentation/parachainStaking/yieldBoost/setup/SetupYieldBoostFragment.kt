package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup

import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.scrollOnFocusTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentYieldBoostSetupBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.view.showRewardEstimation
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class SetupYieldBoostFragment : BaseFragment<SetupYieldBoostViewModel, FragmentYieldBoostSetupBinding>() {

    override val binder by viewBinding(FragmentYieldBoostSetupBinding::bind)

    override fun initViews() {
        binder.setupYieldBoostToolbar.applyStatusBarInsets()

        binder.setupYieldBoostToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.setupYieldBoostContinue.prepareForProgress(viewLifecycleOwner)
        binder.setupYieldBoostContinue.setOnClickListener { viewModel.nextClicked() }

        binder.setupYieldBoostCollator.setOnClickListener { viewModel.selectCollatorClicked() }

        binder.setupYieldBoostOn.setOnClickListener { viewModel.yieldBoostStateChanged(yieldBoostOn = true) }
        binder.setupYieldBoostOff.setOnClickListener { viewModel.yieldBoostStateChanged(yieldBoostOn = false) }

        binder.setupYieldBoostContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        binder.setupYieldBoostScrollArea.scrollOnFocusTo(binder.setupYieldBoostThreshold)

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
        observeValidations(viewModel)
        setupAmountChooser(viewModel.boostThresholdChooserMixin, binder.setupYieldBoostThreshold)
        setupFeeLoading(viewModel, binder.setupYieldBoostFee)

        viewModel.selectedCollatorModel.observe {
            binder.setupYieldBoostCollator.setSelectedCollator(it)
        }

        viewModel.chooseCollatorAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                stakedCollatorSelected = { _, item -> action.onSuccess(item) },
                onCancel = action.onCancel,
                newStakeTargetClicked = null
            ).show()
        }

        viewModel.configurationUi.observe { state ->
            setYieldViewsVisible(state is YieldBoostStateModel.On)

            binder.setupYieldBoostOn.setChecked(state is YieldBoostStateModel.On)
            binder.setupYieldBoostOff.setChecked(state is YieldBoostStateModel.Off)

            if (state is YieldBoostStateModel.On) {
                binder.setupYieldBoostFrequency.text = state.frequencyTitle
            }
        }

        viewModel.buttonState.observe(binder.setupYieldBoostContinue::setState)

        viewModel.rewardsWithYieldBoost.observe(binder.setupYieldBoostOn::showRewardEstimation)
        viewModel.rewardsWithoutYieldBoost.observe(binder.setupYieldBoostOff::showRewardEstimation)
    }

    private fun setYieldViewsVisible(visible: Boolean) {
        listOf(
            binder.setupYieldBoostFrequency,
            binder.setupYieldBoostThreshold,
            binder.setupYieldBoostOakLogo
        ).onEach { it.isVisible = visible }
    }
}
