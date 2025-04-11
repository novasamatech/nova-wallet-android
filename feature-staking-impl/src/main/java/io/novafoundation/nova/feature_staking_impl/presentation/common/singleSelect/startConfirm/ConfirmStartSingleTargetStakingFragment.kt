package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingStartConfirmBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

abstract class ConfirmStartSingleTargetStakingFragment<V : ConfirmStartSingleTargetStakingViewModel<*>> :
    BaseFragment<V, FragmentParachainStakingStartConfirmBinding>() {

    override fun createBinding() = FragmentParachainStakingStartConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmStartParachainStakingContainer.applyStatusBarInsets()

        binder.confirmStartParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.confirmStartParachainStakingExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmStartParachainStakingConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmStartParachainStakingConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.confirmStartParachainStakingCollator.setOnClickListener { viewModel.stakeTargetClicked() }
    }

    override fun subscribe(viewModel: V) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeLoaderMixin, binder.confirmStartParachainStakingExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, binder.confirmStartParachainStakingHints)

        viewModel.title.observe(binder.confirmStartParachainStakingToolbar::setTitle)
        viewModel.showNextProgress.observe(binder.confirmStartParachainStakingConfirm::setProgressState)

        viewModel.amountModel.observe { amountModel ->
            binder.confirmStartParachainStakingAmount.setAmount(amountModel)
            binder.confirmStartParachainStakingAmount.makeVisible()
        }

        viewModel.currentAccountModelFlow.observe(binder.confirmStartParachainStakingExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.confirmStartParachainStakingExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(binder.confirmStartParachainStakingCollator::showAddress)
        viewModel.amountModel.observe(binder.confirmStartParachainStakingAmount::setAmount)
    }
}
