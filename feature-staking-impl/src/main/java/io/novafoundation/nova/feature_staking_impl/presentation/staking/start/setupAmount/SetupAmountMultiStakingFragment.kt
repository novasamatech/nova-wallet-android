package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.makeGoneViews
import io.novafoundation.nova.common.utils.makeVisibleViews
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStartMultiStakingAmountBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.model.StakingPropertiesModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser

class SetupAmountMultiStakingFragment : BaseFragment<SetupAmountMultiStakingViewModel, FragmentStartMultiStakingAmountBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "SetupAmountMultiStakingFragment.payload"

        fun getBundle(payload: SetupAmountMultiStakingPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentStartMultiStakingAmountBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.startMultiStakingSetupAmountToolbar.setHomeButtonListener { viewModel.back() }

        binder.startMultiStakingSetupAmountContinue.prepareForProgress(viewLifecycleOwner)
        binder.startMultiStakingSetupAmountContinue.setOnClickListener { viewModel.continueClicked() }

        binder.startMultiStakingSetupAmountSelection.setOnClickListener { viewModel.selectionClicked() }

        binder.startMultiStakingSetupAmountAmount.amountInput.showSoftKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binder.startMultiStakingSetupAmountAmount.amountInput.hideSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setupAmountMultiStakingComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SetupAmountMultiStakingViewModel) {
        setupAmountChooser(viewModel.amountChooserMixin, binder.startMultiStakingSetupAmountAmount)
        observeValidations(viewModel)

        viewModel.stakingPropertiesModel.observe(::showStakingProperties)
        viewModel.title.observe(binder.startMultiStakingSetupAmountToolbar::setTitle)

        viewModel.continueButtonState.observe(binder.startMultiStakingSetupAmountContinue::setState)
    }

    private fun showStakingProperties(properties: StakingPropertiesModel) {
        when (properties) {
            StakingPropertiesModel.Hidden -> {
                makeGoneViews(binder.startMultiStakingSetupAmountSelection, binder.startMultiStakingSetupAmountRewards)
            }

            is StakingPropertiesModel.Loaded -> {
                makeVisibleViews(binder.startMultiStakingSetupAmountSelection, binder.startMultiStakingSetupAmountRewards)
                binder.startMultiStakingSetupAmountSelection.setModel(properties.content.selection)
                binder.startMultiStakingSetupAmountRewards.showEarnings(properties.content.estimatedReward)
            }

            StakingPropertiesModel.Loading -> {
                makeVisibleViews(binder.startMultiStakingSetupAmountSelection, binder.startMultiStakingSetupAmountRewards)
                binder.startMultiStakingSetupAmountSelection.setLoadingState()
                binder.startMultiStakingSetupAmountRewards.showLoading()
            }
        }
    }
}
