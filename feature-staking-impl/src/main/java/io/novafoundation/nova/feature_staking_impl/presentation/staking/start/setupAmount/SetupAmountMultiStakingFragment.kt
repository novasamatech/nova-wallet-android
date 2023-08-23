package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGoneViews
import io.novafoundation.nova.common.utils.makeVisibleViews
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.model.StakingPropertiesModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import kotlinx.android.synthetic.main.fragment_start_multi_staking_amount.startMultiStakingSetupAmountAmount
import kotlinx.android.synthetic.main.fragment_start_multi_staking_amount.startMultiStakingSetupAmountContinue
import kotlinx.android.synthetic.main.fragment_start_multi_staking_amount.startMultiStakingSetupAmountRewards
import kotlinx.android.synthetic.main.fragment_start_multi_staking_amount.startMultiStakingSetupAmountSelection
import kotlinx.android.synthetic.main.fragment_start_multi_staking_amount.startMultiStakingSetupAmountToolbar

class SetupAmountMultiStakingFragment : BaseFragment<SetupAmountMultiStakingViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "SetupAmountMultiStakingFragment.payload"

        fun getBundle(payload: SetupAmountMultiStakingPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_start_multi_staking_amount, container, false)
    }

    override fun initViews() {
        startMultiStakingSetupAmountToolbar.applyStatusBarInsets()
        startMultiStakingSetupAmountToolbar.setHomeButtonListener { viewModel.back() }

        startMultiStakingSetupAmountContinue.prepareForProgress(viewLifecycleOwner)
        startMultiStakingSetupAmountContinue.setOnClickListener { viewModel.continueClicked() }

        startMultiStakingSetupAmountSelection.setOnClickListener { viewModel.selectionClicked() }
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
        setupAmountChooser(viewModel.amountChooserMixin, startMultiStakingSetupAmountAmount)

        viewModel.stakingPropertiesModel.observe(::showStakingProperties)
        viewModel.title.observe(startMultiStakingSetupAmountToolbar::setTitle)

        viewModel.continueButtonState.observe(startMultiStakingSetupAmountContinue::setState)
    }

    private fun showStakingProperties(properties: StakingPropertiesModel) {
        when (properties) {
            StakingPropertiesModel.Hidden -> {
                makeGoneViews(startMultiStakingSetupAmountSelection, startMultiStakingSetupAmountRewards)
            }

            is StakingPropertiesModel.Loaded -> {
                makeVisibleViews(startMultiStakingSetupAmountSelection, startMultiStakingSetupAmountRewards)
                startMultiStakingSetupAmountSelection.setModel(properties.content.selection)
                startMultiStakingSetupAmountRewards.showEarnings(properties.content.estimatedReward)
            }

            StakingPropertiesModel.Loading -> {
                makeVisibleViews(startMultiStakingSetupAmountSelection, startMultiStakingSetupAmountRewards)
                startMultiStakingSetupAmountSelection.setLoadingState()
                startMultiStakingSetupAmountRewards.showLoading()
            }
        }
    }
}
