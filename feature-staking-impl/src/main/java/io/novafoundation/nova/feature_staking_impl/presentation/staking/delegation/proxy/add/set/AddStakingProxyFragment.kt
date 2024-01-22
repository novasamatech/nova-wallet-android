package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.set

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupExternalAccounts
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.setupYourWalletsBtn
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.addProxyToolbar
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.setStakingProxyAddress
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.addStakingProxyButton
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.setStakingProxyContainer
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.addStakingProxyDeposit
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.addStakingProxyFee
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.addStakingProxyTitle
import kotlinx.android.synthetic.main.fragment_add_staking_proxy.addStakingProxySelectWallet

class AddStakingProxyFragment : BaseFragment<AddStakingProxyViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_add_staking_proxy, container, false)
    }

    override fun initViews() {
        setStakingProxyContainer.applyStatusBarInsets()
        addProxyToolbar.setHomeButtonListener { viewModel.backClicked() }
        addStakingProxyButton.prepareForProgress(this)

        addStakingProxySelectWallet.setOnClickListener { viewModel.selectAuthorityWallet() }
        addStakingProxyButton.setOnClickListener { viewModel.onConfirmClick() }
        addStakingProxyDeposit.setOnClickListener { viewModel.showProxyDepositDescription() }
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

        setupAddressInput(viewModel.addressInputMixin, setStakingProxyAddress)
        setupExternalAccounts(viewModel.addressInputMixin, setStakingProxyAddress)
        setupYourWalletsBtn(addStakingProxySelectWallet, viewModel.selectAddressMixin)

        viewModel.titleFlow.observe {
            addStakingProxyTitle.text = it
        }

        viewModel.proxyDepositModel.observe {
            addStakingProxyDeposit.showAmount(it)
        }

        setupFeeLoading(viewModel.feeMixin, addStakingProxyFee)

        viewModel.continueButtonState.observe(addStakingProxyButton::setState)
    }
}
