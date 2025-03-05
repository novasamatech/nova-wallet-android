package io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_mythos_claim_rewards.mythosClaimRewardsAmount
import kotlinx.android.synthetic.main.fragment_mythos_claim_rewards.mythosClaimRewardsConfirm
import kotlinx.android.synthetic.main.fragment_mythos_claim_rewards.mythosClaimRewardsExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_mythos_claim_rewards.mythosClaimRewardsToolbar

class MythosClaimRewardsFragment : BaseFragment<MythosClaimRewardsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_mythos_claim_rewards, container, false)
    }

    override fun initViews() {
        mythosClaimRewardsToolbar.applyStatusBarInsets()

        mythosClaimRewardsExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        mythosClaimRewardsToolbar.setHomeButtonListener { viewModel.backClicked() }
        mythosClaimRewardsConfirm.prepareForProgress(viewLifecycleOwner)
        mythosClaimRewardsConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .claimMythosRewardsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MythosClaimRewardsViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        viewModel.feeLoaderMixin.setupFeeLoading(mythosClaimRewardsExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(mythosClaimRewardsConfirm::setProgressState)

        viewModel.pendingRewardsAmountModel.observe(mythosClaimRewardsAmount::setAmount)

        viewModel.walletUiFlow.observe(mythosClaimRewardsExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(mythosClaimRewardsExtrinsicInformation::setAccount)
    }
}
