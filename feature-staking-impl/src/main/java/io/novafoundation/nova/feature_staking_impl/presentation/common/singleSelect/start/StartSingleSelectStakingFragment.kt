package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start

import androidx.annotation.CallSuper
import io.novafoundation.nova.common.address.WithAccountId
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingStartBinding
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards.setupParachainStakingRewardsComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

abstract class StartSingleSelectStakingFragment<T, V : StartSingleSelectStakingViewModel<T, *>> : BaseFragment<V, FragmentParachainStakingStartBinding>()
    where T : Identifiable, T : WithAccountId {

    override fun createBinding() = FragmentParachainStakingStartBinding.inflate(layoutInflater)

    @CallSuper
    override fun initViews() {
        binder.startParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.startParachainStakingNext.prepareForProgress(viewLifecycleOwner)
        binder.startParachainStakingNext.setOnClickListener { viewModel.nextClicked() }

        binder.startParachainStakingCollator.setOnClickListener { viewModel.selectTargetClicked() }
    }

    override fun subscribe(viewModel: V) {
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.startParachainStakingAmountField)
        setupParachainStakingRewardsComponent(viewModel.rewardsComponent, binder.startParachainStakingRewards)
        setupFeeLoading(viewModel.originFeeMixin, binder.startParachainStakingFee)
        observeHints(viewModel.hintsMixin, binder.startParachainStakingHints)

        viewModel.title.observe(binder.startParachainStakingToolbar::setTitle)

        viewModel.selectedTargetModelFlow.observe {
            binder.startParachainStakingCollator.setSelectedTarget(it)
        }

        viewModel.buttonState.observe(binder.startParachainStakingNext::setState)

        viewModel.minimumStake.observe(binder.startParachainStakingMinStake::showAmount)

        viewModel.chooseTargetAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                onResponse = action.onSuccess,
                onCancel = action.onCancel,
            ).show()
        }
    }
}
