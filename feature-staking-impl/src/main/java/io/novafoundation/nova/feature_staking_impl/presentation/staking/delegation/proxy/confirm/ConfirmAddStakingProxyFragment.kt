package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyButton
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyDelegationAccount
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyDeposit
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyNetwork
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyNetworkFee
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyProxiedAccount
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyToolbar
import kotlinx.android.synthetic.main.fragment_confirm_add_staking_proxy.confirmAddStakingProxyWallet

class ConfirmAddStakingProxyFragment : BaseFragment<ConfirmAddStakingProxyViewModel>() {
    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(payload: ConfirmAddStakingProxyPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_add_staking_proxy, container, false)
    }

    override fun initViews() {
        confirmAddStakingProxyToolbar.applyStatusBarInsets()

        confirmAddStakingProxyToolbar.setHomeButtonListener { viewModel.back() }

        confirmAddStakingProxyButton.setOnClickListener { viewModel.confirmClicked() }
        confirmAddStakingProxyButton.prepareForProgress(viewLifecycleOwner)

        confirmAddStakingProxyProxiedAccount.setOnValueClickListener { viewModel.proxiedAccountClicked() }
        confirmAddStakingProxyDeposit.setOnClickListener { viewModel.depositClicked() }
        confirmAddStakingProxyDelegationAccount.setOnClickListener { viewModel.proxyAccountClicked() }
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

        viewModel.chainModel.observe { confirmAddStakingProxyNetwork.showChain(it) }
        viewModel.walletUiFlow.observe { confirmAddStakingProxyWallet.showWallet(it) }
        viewModel.proxiedAccountModel.observe { confirmAddStakingProxyProxiedAccount.showAddress(it) }
        viewModel.proxyDeposit.observe { confirmAddStakingProxyDeposit.showAmount(it) }
        viewModel.feeModelFlow.observe { confirmAddStakingProxyNetworkFee.showAmount(it) }
        viewModel.proxyAccountModel.observe { confirmAddStakingProxyDelegationAccount.showAddress(it) }

        viewModel.validationProgressFlow.observe(confirmAddStakingProxyButton::setProgress)
    }
}
