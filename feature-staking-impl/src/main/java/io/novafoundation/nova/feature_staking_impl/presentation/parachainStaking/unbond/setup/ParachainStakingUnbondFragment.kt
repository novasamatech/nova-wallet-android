package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingUnbondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class ParachainStakingUnbondFragment : BaseFragment<ParachainStakingUnbondViewModel, FragmentParachainStakingUnbondBinding>() {

    override val binder by viewBinding(FragmentParachainStakingUnbondBinding::bind)

    override fun initViews() {
        binder.parachainStakingUnbondContainer.applyStatusBarInsets()

        binder.parachainStakingUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.parachainStakingUnbondNext.prepareForProgress(viewLifecycleOwner)
        binder.parachainStakingUnbondNext.setOnClickListener { viewModel.nextClicked() }

        binder.parachainStakingUnbondCollator.setOnClickListener { viewModel.selectCollatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingUnbondSetupFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingUnbondViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.parachainStakingUnbondAmountField)
        setupFeeLoading(viewModel, binder.parachainStakingUnbondFee)
        observeHints(viewModel.hintsMixin, binder.parachainStakingUnbondHints)

        viewModel.selectedCollatorModel.observe(binder.parachainStakingUnbondCollator::setSelectedCollator)

        viewModel.buttonState.observe(binder.parachainStakingUnbondNext::setState)

        viewModel.minimumStake.observe(binder.parachainStakingUnbondMinStake::showAmount)
        viewModel.transferable.observe(binder.parachainStakingUnbondTransferable::showAmount)

        viewModel.chooseCollatorAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                stakedCollatorSelected = { _, item -> action.onSuccess(item) },
                onCancel = action.onCancel,
                newStakeTargetClicked = null
            ).show()
        }
    }
}
