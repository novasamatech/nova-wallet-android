package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ParachainStakingRedeemFragment : BaseFragment<ParachainStakingRedeemViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_redeem, container, false)
    }

    override fun initViews() {
        parachainStakingRedeemContainer.applyStatusBarInsets()

        parachainStakingRedeemToolbar.setHomeButtonListener { viewModel.backClicked() }

        parachainStakingRedeemExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        parachainStakingRedeemConfirm.prepareForProgress(viewLifecycleOwner)
        parachainStakingRedeemConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingRedeemFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingRedeemViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, parachainStakingRedeemExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(parachainStakingRedeemConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(parachainStakingRedeemExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(parachainStakingRedeemExtrinsicInfo::setWallet)

        viewModel.redeemableAmount.observe(parachainStakingRedeemAmount::showLoadingState)
    }
}
