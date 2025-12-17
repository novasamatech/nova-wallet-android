package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmAddStakingProxyBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class ConfirmAddStakingProxyFragment : BaseFragment<ConfirmAddStakingProxyViewModel, FragmentConfirmAddStakingProxyBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(payload: ConfirmAddStakingProxyPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentConfirmAddStakingProxyBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmAddStakingProxyToolbar.setHomeButtonListener { viewModel.back() }

        binder.confirmAddStakingProxyButton.setOnClickListener { viewModel.confirmClicked() }
        binder.confirmAddStakingProxyButton.prepareForProgress(viewLifecycleOwner)

        binder.confirmAddStakingProxyProxiedAccount.setOnClickListener { viewModel.proxiedAccountClicked() }
        binder.confirmAddStakingProxyDeposit.setOnClickListener { viewModel.depositClicked() }
        binder.confirmAddStakingProxyDelegationAccount.setOnClickListener { viewModel.proxyAccountClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmAddStakingProxyPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmAddStakingProxyFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmAddStakingProxyViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeDescription(viewModel)

        viewModel.chainModel.observe { binder.confirmAddStakingProxyNetwork.showChain(it) }
        viewModel.walletUiFlow.observe { binder.confirmAddStakingProxyWallet.showWallet(it) }
        viewModel.proxiedAccountModel.observe { binder.confirmAddStakingProxyProxiedAccount.showAddress(it) }
        viewModel.proxyDeposit.observe { binder.confirmAddStakingProxyDeposit.showAmount(it) }
        viewModel.feeModelFlow.observe { binder.confirmAddStakingProxyNetworkFee.showAmount(it) }
        viewModel.proxyAccountModel.observe { binder.confirmAddStakingProxyDelegationAccount.showAddress(it) }

        viewModel.validationProgressFlow.observe(binder.confirmAddStakingProxyButton::setProgressState)
    }
}
