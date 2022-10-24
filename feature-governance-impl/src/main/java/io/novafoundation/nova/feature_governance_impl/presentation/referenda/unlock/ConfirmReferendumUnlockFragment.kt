package io.novafoundation.nova.feature_governance_impl.presentation.referenda.unlock

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
import kotlinx.android.synthetic.main.fragment_referendum_confirm_unlock.confirmReferendumUnlockConfirm
import kotlinx.android.synthetic.main.fragment_referendum_confirm_unlock.confirmReferendumUnlockInformation
import kotlinx.android.synthetic.main.fragment_referendum_confirm_unlock.confirmReferendumUnlockToolbar

class ConfirmReferendumUnlockFragment : BaseFragment<ConfirmReferendumUnlockViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_confirm_unlock, container, false)
    }

    override fun initViews() {
        confirmReferendumUnlockToolbar.setHomeButtonListener { viewModel.backClicked() }
        confirmReferendumUnlockConfirm.prepareForProgress(viewLifecycleOwner)
        confirmReferendumUnlockConfirm.setOnClickListener { viewModel.confirmClicked() }
        confirmReferendumUnlockInformation.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .confirmReferendumUnlock()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmReferendumUnlockViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        // TODO observeHints(viewModel.hintsMixin, confirmReferendumUnlockHints)

        setupFeeLoading(viewModel, confirmReferendumUnlockInformation.fee)

        viewModel.addressModel.observe(confirmReferendumUnlockInformation::setAccount)
        viewModel.walletModel.observe(confirmReferendumUnlockInformation::setWallet)

        viewModel.showNextProgress.observe(confirmReferendumUnlockConfirm::setProgress)
    }
}
