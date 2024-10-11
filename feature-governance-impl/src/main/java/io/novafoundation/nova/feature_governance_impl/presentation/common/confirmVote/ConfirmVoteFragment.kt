package io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

abstract class ConfirmVoteFragment<T : ConfirmVoteViewModel> : BaseFragment<T>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_confirm_vote, container, false)
    }

    override fun initViews() {
        confirmReferendumVoteToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        confirmReferendumVoteConfirm.prepareForProgress(viewLifecycleOwner)
        confirmReferendumVoteConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmReferendumVoteInformation.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun subscribe(viewModel: T) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, confirmReferendumVoteHints)

        setupFeeLoading(viewModel, confirmReferendumVoteInformation.fee)

        viewModel.currentAddressModelFlow.observe(confirmReferendumVoteInformation::setAccount)
        viewModel.walletModel.observe(confirmReferendumVoteInformation::setWallet)

        viewModel.amountModelFlow.observe(confirmReferendumVoteAmount::setAmount)

        viewModel.accountVoteUi.observe(confirmReferendumVoteResult::setModel)

        viewModel.titleFlow.observe(confirmReferendumVoteToolbar::setTitle)

        viewModel.locksChangeUiFlow.observe {
            confirmReferendumVoteLockedAmountChanges.setAmountChangeModel(it.amountChange)
            confirmReferendumVoteLockedPeriodChanges.setAmountChangeModel(it.periodChange)
            confirmReferendumVoteTransferableAmountChanges.setAmountChangeModel(it.transferableChange)
        }

        viewModel.showNextProgress.observe(confirmReferendumVoteConfirm::setProgressState)
    }
}
