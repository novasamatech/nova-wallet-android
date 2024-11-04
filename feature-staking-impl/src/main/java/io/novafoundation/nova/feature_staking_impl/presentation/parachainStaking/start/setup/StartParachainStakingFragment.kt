package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingStartBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.ChooseCollatorResponse
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.setupParachainStakingRewardsComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class StartParachainStakingFragment : BaseFragment<StartParachainStakingViewModel, FragmentParachainStakingStartBinding>() {

    companion object {

        private const val PAYLOAD = "StartParachainStakingFragment.Payload"

        fun getBundle(payload: StartParachainStakingPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentParachainStakingStartBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.startParachainStakingContainer.applyStatusBarInsets()

        binder.startParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.startParachainStakingNext.prepareForProgress(viewLifecycleOwner)
        binder.startParachainStakingNext.setOnClickListener { viewModel.nextClicked() }

        binder.startParachainStakingCollator.setOnClickListener { viewModel.selectCollatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startParachainStakingFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: StartParachainStakingViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.startParachainStakingAmountField)
        setupParachainStakingRewardsComponent(viewModel.rewardsComponent, binder.startParachainStakingRewards)
        setupFeeLoading(viewModel, binder.startParachainStakingFee)
        observeHints(viewModel.hintsMixin, binder.startParachainStakingHints)

        viewModel.title.observe(binder.startParachainStakingToolbar::setTitle)

        viewModel.selectedCollatorModel.observe {
            binder.startParachainStakingCollator.setSelectedCollator(it)
        }

        viewModel.buttonState.observe(binder.startParachainStakingNext::setState)

        viewModel.minimumStake.observe(binder.startParachainStakingMinStake::showAmount)

        viewModel.chooseCollatorAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                stakedCollatorSelected = { _, item -> action.onSuccess(ChooseCollatorResponse.Existing(item)) },
                onCancel = action.onCancel,
                newStakeTargetClicked = { _, _ -> action.onSuccess(ChooseCollatorResponse.New) }
            ).show()
        }
    }
}
