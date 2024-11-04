package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentGovernanceConfirmUnlockBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmGovernanceUnlockFragment : BaseFragment<ConfirmGovernanceUnlockViewModel, FragmentGovernanceConfirmUnlockBinding>() {

    override val binder by viewBinding(FragmentGovernanceConfirmUnlockBinding::bind)

    override fun initViews() {
        binder.confirmGovernanceUnlockToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.confirmGovernanceUnlockConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmGovernanceUnlockConfirm.setOnClickListener { viewModel.confirmClicked() }
        binder.confirmGovernanceUnlockInformation.setOnAccountClickedListener { viewModel.accountClicked() }
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
        observeHints(viewModel.hintsMixin, binder.confirmReferendumUnlockHints)
        setupFeeLoading(viewModel, binder.confirmGovernanceUnlockInformation.fee)

        viewModel.currentAddressModelFlow.observe(binder.confirmGovernanceUnlockInformation::setAccount)
        viewModel.walletModel.observe(binder.confirmGovernanceUnlockInformation::setWallet)

        viewModel.amountModelFlow.observe(binder.confirmReferendumUnlockAmount::setAmount)

        viewModel.transferableChange.observe(binder.confirmReferendumUnlockTransferableChange::setAmountChangeModel)
        viewModel.governanceLockChange.observe(binder.confirmReferendumUnlockGovernanceLockChange::setAmountChangeModel)

        viewModel.confirmButtonState.observe(binder.confirmGovernanceUnlockConfirm::setState)
    }
}
