package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmChangeValidatorsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmChangeValidatorsFragment : BaseFragment<ConfirmChangeValidatorsViewModel, FragmentConfirmChangeValidatorsBinding>() {

    override val binder by viewBinding(FragmentConfirmChangeValidatorsBinding::bind)

    override fun initViews() {
        binder.stakingConfirmationContainer.applyStatusBarInsets()

        binder.confirmChangeValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.confirmChangeValidatorsAccount.setOnClickListener { viewModel.originAccountClicked() }

        binder.confirmChangeValidatorsConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmChangeValidatorsConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.confirmChangeValidatorsValidators.setOnClickListener { viewModel.nominationsClicked() }
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
        setupFeeLoading(viewModel, binder.confirmChangeValidatorsFee)
        observeHints(viewModel.hintsMixin, binder.confirmChangeValidatorsHints)

        viewModel.showNextProgress.observe(binder.confirmChangeValidatorsConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.confirmChangeValidatorsAccount::showAddress)
        viewModel.walletFlow.observe(binder.confirmChangeValidatorsWallet::showWallet)

        viewModel.nominationsFlow.observe {
            binder.confirmChangeValidatorsValidators.showValue(it)
        }
    }
}
