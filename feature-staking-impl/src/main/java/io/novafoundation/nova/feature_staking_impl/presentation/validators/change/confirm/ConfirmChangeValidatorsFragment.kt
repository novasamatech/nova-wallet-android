package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsAccount
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsConfirm
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsFee
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsHints
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsToolbar
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsValidators
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.confirmChangeValidatorsWallet
import kotlinx.android.synthetic.main.fragment_confirm_change_validators.stakingConfirmationContainer

class ConfirmChangeValidatorsFragment : BaseFragment<ConfirmChangeValidatorsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_change_validators, container, false)
    }

    override fun initViews() {
        stakingConfirmationContainer.applyStatusBarInsets()

        confirmChangeValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        confirmChangeValidatorsAccount.setOnClickListener { viewModel.originAccountClicked() }

        confirmChangeValidatorsConfirm.prepareForProgress(viewLifecycleOwner)
        confirmChangeValidatorsConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmChangeValidatorsValidators.setOnClickListener { viewModel.nominationsClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmStakingComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmChangeValidatorsViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, confirmChangeValidatorsFee)
        observeHints(viewModel.hintsMixin, confirmChangeValidatorsHints)

        viewModel.showNextProgress.observe(confirmChangeValidatorsConfirm::setProgress)

        viewModel.currentAccountModelFlow.observe(confirmChangeValidatorsAccount::showAddress)
        viewModel.walletFlow.observe(confirmChangeValidatorsWallet::showWallet)

        viewModel.nominationsFlow.observe {
            confirmChangeValidatorsValidators.showValue(it)
        }
    }
}
