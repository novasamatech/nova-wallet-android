package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmGovernanceUnlockConfirm
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmGovernanceUnlockInformation
import kotlinx.android.synthetic.main.fragment_governance_confirm_unlock.confirmGovernanceUnlockToolbar

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
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        // TODO observeHints(viewModel.hintsMixin, confirmReferendumUnlockHints)

        setupFeeLoading(viewModel, confirmGovernanceUnlockInformation.fee)

        viewModel.addressModel.observe(confirmGovernanceUnlockInformation::setAccount)
        viewModel.walletModel.observe(confirmGovernanceUnlockInformation::setWallet)

        viewModel.showNextProgress.observe(confirmGovernanceUnlockConfirm::setProgress)
    }
}
