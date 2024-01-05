package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupExternalAccounts
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_set_controller_account.confirmSetControllerContainer
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.setStakingProxyAddress
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.setStakingProxyButton
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.setStakingProxyContainer
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.setStakingProxyDeposit
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.setStakingProxyFee
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.setStakingProxyTitle
import kotlinx.android.synthetic.main.fragment_set_staking_proxy.stakingProxySelectWallet

class SetStakingProxyFragment : BaseFragment<SetStakingProxyViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_set_staking_proxy, container, false)
    }

    override fun initViews() {
        setStakingProxyContainer.applyStatusBarInsets()

        stakingProxySelectWallet.setOnClickListener { viewModel.selectRecipientWallet() }
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

    override fun subscribe(viewModel: SetStakingProxyViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)

        setupAddressInput(viewModel.addressInputMixin, setStakingProxyAddress)
        setupExternalAccounts(viewModel.addressInputMixin, setStakingProxyAddress)

        viewModel.titleFlow.observe {
            setStakingProxyTitle.text = it
        }

        viewModel.isSelectAddressAvailable.observe {
            stakingProxySelectWallet.isInvisible = !it
        }

        viewModel.proxyDeposit.observe {
            setStakingProxyDeposit.showAmount(it)
        }

        setupFeeLoading(viewModel.feeMixin, setStakingProxyFee)

        viewModel.continueButtonState.observe(setStakingProxyButton::setState)
    }
}
