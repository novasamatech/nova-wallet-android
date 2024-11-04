package io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentReferendumConfirmVoteBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

abstract class ConfirmVoteFragment<T : ConfirmVoteViewModel> : BaseFragment<T, FragmentReferendumConfirmVoteBinding>() {

    override fun createBinding() = FragmentReferendumConfirmVoteBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmReferendumVoteToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.confirmReferendumVoteConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmReferendumVoteConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.confirmReferendumVoteInformation.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun subscribe(viewModel: T) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.confirmReferendumVoteHints)

        setupFeeLoading(viewModel, binder.confirmReferendumVoteInformation.fee)

        viewModel.currentAddressModelFlow.observe(binder.confirmReferendumVoteInformation::setAccount)
        viewModel.walletModel.observe(binder.confirmReferendumVoteInformation::setWallet)

        viewModel.amountModelFlow.observe(binder.confirmReferendumVoteAmount::setAmount)

        viewModel.accountVoteUi.observe(binder.confirmReferendumVoteResult::setModel)

        viewModel.titleFlow.observe(binder.confirmReferendumVoteToolbar::setTitle)

        viewModel.locksChangeUiFlow.observe {
            binder.confirmReferendumVoteLockedAmountChanges.setAmountChangeModel(it.amountChange)
            binder.confirmReferendumVoteLockedPeriodChanges.setAmountChangeModel(it.periodChange)
            binder.confirmReferendumVoteTransferableAmountChanges.setAmountChangeModel(it.transferableChange)
        }

        viewModel.showNextProgress.observe(binder.confirmReferendumVoteConfirm::setProgressState)
    }
}
