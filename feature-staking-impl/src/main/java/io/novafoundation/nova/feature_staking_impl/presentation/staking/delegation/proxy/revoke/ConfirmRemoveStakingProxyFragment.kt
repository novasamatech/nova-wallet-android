package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmRemoveStakingProxyFragment : BaseFragment<ConfirmRemoveStakingProxyViewModel>() {
    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(payload: ConfirmRemoveStakingProxyPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_revoke_staking_proxy, container, false)
    }

    override fun initViews() {
        confirmRemoveStakingProxyToolbar.applyStatusBarInsets()

        confirmRemoveStakingProxyToolbar.setHomeButtonListener { viewModel.backClicked() }

        confirmRemoveStakingProxyButton.setOnClickListener { viewModel.confirmClicked() }
        confirmRemoveStakingProxyButton.prepareForProgress(viewLifecycleOwner)

        confirmRemoveStakingProxyProxiedAccount.setOnClickListener { viewModel.proxiedAccountClicked() }
        confirmRemoveStakingProxyDelegationAccount.setOnClickListener { viewModel.proxyAccountClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmRemoveStakingProxyPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmRevokeStakingProxyFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmRemoveStakingProxyViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeMixin, confirmRemoveStakingProxyNetworkFee)

        viewModel.chainModel.observe { confirmRemoveStakingProxyNetwork.showChain(it) }
        viewModel.walletUiFlow.observe { confirmRemoveStakingProxyWallet.showWallet(it) }
        viewModel.proxiedAccountModel.observe { confirmRemoveStakingProxyProxiedAccount.showAddress(it) }
        viewModel.proxyAccountModel.observe { confirmRemoveStakingProxyDelegationAccount.showAddress(it) }

        viewModel.validationProgressFlow.observe(confirmRemoveStakingProxyButton::setProgressState)
    }
}
