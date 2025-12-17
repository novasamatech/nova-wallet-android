package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.set

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupExternalAccounts
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.setupYourWalletsBtn
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentAddStakingProxyBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class AddStakingProxyFragment : BaseFragment<AddStakingProxyViewModel, FragmentAddStakingProxyBinding>() {

    override fun createBinding() = FragmentAddStakingProxyBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.addProxyToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.addStakingProxyButton.prepareForProgress(this)

        binder.addStakingProxySelectWallet.setOnClickListener { viewModel.selectAuthorityWallet() }
        binder.addStakingProxyButton.setOnClickListener { viewModel.onConfirmClick() }
        binder.addStakingProxyDeposit.setOnClickListener { viewModel.showProxyDepositDescription() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setStakingProxyFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AddStakingProxyViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)
        observeDescription(viewModel)

        setupAddressInput(viewModel.addressInputMixin, binder.setStakingProxyAddress)
        setupExternalAccounts(viewModel.addressInputMixin, binder.setStakingProxyAddress)
        setupYourWalletsBtn(binder.addStakingProxySelectWallet, viewModel.selectAddressMixin)

        viewModel.titleFlow.observe {
            binder.addStakingProxyTitle.text = it
        }

        viewModel.proxyDepositModel.observe {
            binder.addStakingProxyDeposit.showAmount(it)
        }

        setupFeeLoading(viewModel.feeMixin, binder.addStakingProxyFee)

        viewModel.continueButtonState.observe(binder.addStakingProxyButton::setState)
    }
}
