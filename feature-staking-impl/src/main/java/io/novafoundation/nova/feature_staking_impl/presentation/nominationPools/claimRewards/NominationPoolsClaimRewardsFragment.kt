package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_nomination_pools_claim_rewards.nominationPoolsClaimRewardRestakeSwitch
import kotlinx.android.synthetic.main.fragment_nomination_pools_claim_rewards.nominationPoolsClaimRewardsAmount
import kotlinx.android.synthetic.main.fragment_nomination_pools_claim_rewards.nominationPoolsClaimRewardsConfirm
import kotlinx.android.synthetic.main.fragment_nomination_pools_claim_rewards.nominationPoolsClaimRewardsExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_nomination_pools_claim_rewards.nominationPoolsClaimRewardsToolbar

class NominationPoolsClaimRewardsFragment : BaseFragment<NominationPoolsClaimRewardsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_nomination_pools_claim_rewards, container, false)
    }

    override fun initViews() {
        nominationPoolsClaimRewardsToolbar.applyStatusBarInsets()

        nominationPoolsClaimRewardsExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        nominationPoolsClaimRewardsToolbar.setHomeButtonListener { viewModel.backClicked() }
        nominationPoolsClaimRewardsConfirm.prepareForProgress(viewLifecycleOwner)
        nominationPoolsClaimRewardsConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .nominationPoolsStakingClaimRewards()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NominationPoolsClaimRewardsViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeLoaderMixin, nominationPoolsClaimRewardsExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(nominationPoolsClaimRewardsConfirm::setProgress)

        viewModel.pendingRewardsAmountModel.observe(nominationPoolsClaimRewardsAmount::setAmount)

        viewModel.walletUiFlow.observe(nominationPoolsClaimRewardsExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(nominationPoolsClaimRewardsExtrinsicInformation::setAccount)

        nominationPoolsClaimRewardRestakeSwitch.field.bindTo(viewModel.shouldRestakeInput, viewLifecycleOwner.lifecycleScope)
    }
}
