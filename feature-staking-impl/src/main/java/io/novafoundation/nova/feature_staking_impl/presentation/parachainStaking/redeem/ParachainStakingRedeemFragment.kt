package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingRedeemBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ParachainStakingRedeemFragment : BaseFragment<ParachainStakingRedeemViewModel, FragmentParachainStakingRedeemBinding>() {

    override fun createBinding() = FragmentParachainStakingRedeemBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.parachainStakingRedeemToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.parachainStakingRedeemExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.parachainStakingRedeemConfirm.prepareForProgress(viewLifecycleOwner)
        binder.parachainStakingRedeemConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        setupFeeLoading(viewModel, binder.parachainStakingRedeemExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(binder.parachainStakingRedeemConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.parachainStakingRedeemExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.parachainStakingRedeemExtrinsicInfo::setWallet)

        viewModel.redeemableAmount.observe(binder.parachainStakingRedeemAmount::showLoadingState)
    }
}
