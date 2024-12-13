package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmGovernanceUnlockConfirm
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmGovernanceUnlockInformation
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmGovernanceUnlockToolbar
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmReferendumUnlockAmount
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmReferendumUnlockGovernanceLockChange
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmReferendumUnlockHints
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmReferendumUnlockTransferableChange

class ConfirmGovernanceUnlockFragment : BaseFragment<ConfirmGovernanceUnlockViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_governance_confirm_unlock, container, false)
    }

    override fun initViews() {
        confirmGovernanceUnlockToolbar.setHomeButtonListener { viewModel.backClicked() }
        confirmGovernanceUnlockConfirm.prepareForProgress(viewLifecycleOwner)
        confirmGovernanceUnlockConfirm.setOnClickListener { viewModel.confirmClicked() }
        confirmGovernanceUnlockInformation.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .confirmGovernanceUnlockFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmGovernanceUnlockViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, confirmReferendumUnlockHints)
        setupFeeLoading(viewModel, confirmGovernanceUnlockInformation.fee)

        viewModel.currentAddressModelFlow.observe(confirmGovernanceUnlockInformation::setAccount)
        viewModel.walletModel.observe(confirmGovernanceUnlockInformation::setWallet)

        viewModel.amountModelFlow.observe(confirmReferendumUnlockAmount::setAmount)

        viewModel.transferableChange.observe(confirmReferendumUnlockTransferableChange::setAmountChangeModel)
        viewModel.governanceLockChange.observe(confirmReferendumUnlockGovernanceLockChange::setAmountChangeModel)

        viewModel.confirmButtonState.observe(confirmGovernanceUnlockConfirm::setState)
    }
}
