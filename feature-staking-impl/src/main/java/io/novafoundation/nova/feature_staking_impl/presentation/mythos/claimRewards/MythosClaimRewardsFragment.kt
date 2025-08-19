package io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentMythosClaimRewardsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading

class MythosClaimRewardsFragment : BaseFragment<MythosClaimRewardsViewModel, FragmentMythosClaimRewardsBinding>() {

    override fun createBinding() = FragmentMythosClaimRewardsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.mythosClaimRewardsExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.mythosClaimRewardsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.mythosClaimRewardsConfirm.prepareForProgress(viewLifecycleOwner)
        binder.mythosClaimRewardsConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        viewModel.feeLoaderMixin.setupFeeLoading(binder.mythosClaimRewardsExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(binder.mythosClaimRewardsConfirm::setProgressState)

        viewModel.pendingRewardsAmountModel.observe(binder.mythosClaimRewardsAmount::setAmount)

        viewModel.walletUiFlow.observe(binder.mythosClaimRewardsExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.mythosClaimRewardsExtrinsicInformation::setAccount)

        binder.mythosClaimRewardRestakeSwitch.field.bindTo(viewModel.shouldRestakeFlow, viewLifecycleOwner.lifecycleScope)
    }
}
