package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmRevokeStakingProxyBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmRemoveStakingProxyFragment : BaseFragment<ConfirmRemoveStakingProxyViewModel, FragmentConfirmRevokeStakingProxyBinding>() {
    companion object {

        private const val PAYLOAD_KEY = "PAYLOAD_KEY"

        fun getBundle(payload: ConfirmRemoveStakingProxyPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override val binder by viewBinding(FragmentConfirmRevokeStakingProxyBinding::bind)

    override fun initViews() {
        binder.confirmRemoveStakingProxyToolbar.applyStatusBarInsets()

        binder.confirmRemoveStakingProxyToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.confirmRemoveStakingProxyButton.setOnClickListener { viewModel.confirmClicked() }
        binder.confirmRemoveStakingProxyButton.prepareForProgress(viewLifecycleOwner)

        binder.confirmRemoveStakingProxyProxiedAccount.setOnClickListener { viewModel.proxiedAccountClicked() }
        binder.confirmRemoveStakingProxyDelegationAccount.setOnClickListener { viewModel.proxyAccountClicked() }
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
        setupFeeLoading(viewModel.feeMixin, binder.confirmRemoveStakingProxyNetworkFee)

        viewModel.chainModel.observe { binder.confirmRemoveStakingProxyNetwork.showChain(it) }
        viewModel.walletUiFlow.observe { binder.confirmRemoveStakingProxyWallet.showWallet(it) }
        viewModel.proxiedAccountModel.observe { binder.confirmRemoveStakingProxyProxiedAccount.showAddress(it) }
        viewModel.proxyAccountModel.observe { binder.confirmRemoveStakingProxyDelegationAccount.showAddress(it) }

        viewModel.validationProgressFlow.observe(binder.confirmRemoveStakingProxyButton::setProgressState)
    }
}
