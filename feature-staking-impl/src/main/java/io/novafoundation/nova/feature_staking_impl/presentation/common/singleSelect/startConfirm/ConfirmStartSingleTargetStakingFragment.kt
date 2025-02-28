package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingAmount
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingCollator
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingConfirm
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingHints
import kotlinx.android.synthetic.main.fragment_parachain_staking_start_confirm.confirmStartParachainStakingToolbar

abstract class ConfirmStartSingleTargetStakingFragment<V : ConfirmStartSingleTargetStakingViewModel<*>> : BaseFragment<V>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_start_confirm, container, false)
    }

    override fun initViews() {
        confirmStartParachainStakingContainer.applyStatusBarInsets()

        confirmStartParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        confirmStartParachainStakingExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        confirmStartParachainStakingConfirm.prepareForProgress(viewLifecycleOwner)
        confirmStartParachainStakingConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmStartParachainStakingCollator.setOnClickListener { viewModel.stakeTargetClicked() }
    }

    override fun subscribe(viewModel: V) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, confirmStartParachainStakingExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, confirmStartParachainStakingHints)

        viewModel.title.observe(confirmStartParachainStakingToolbar::setTitle)
        viewModel.showNextProgress.observe(confirmStartParachainStakingConfirm::setProgressState)

        viewModel.amountModel.observe { amountModel ->
            confirmStartParachainStakingAmount.setAmount(amountModel)
            confirmStartParachainStakingAmount.makeVisible()
        }

        viewModel.currentAccountModelFlow.observe(confirmStartParachainStakingExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(confirmStartParachainStakingExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(confirmStartParachainStakingCollator::showAddress)
        viewModel.amountModel.observe(confirmStartParachainStakingAmount::setAmount)
    }
}
