package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentNominationPoolsClaimRewardsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class NominationPoolsClaimRewardsFragment : BaseFragment<NominationPoolsClaimRewardsViewModel, FragmentNominationPoolsClaimRewardsBinding>() {

    override val binder by viewBinding(FragmentNominationPoolsClaimRewardsBinding::bind)

    override fun initViews() {
        binder.nominationPoolsClaimRewardsToolbar.applyStatusBarInsets()

        binder.nominationPoolsClaimRewardsExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.nominationPoolsClaimRewardsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.nominationPoolsClaimRewardsConfirm.prepareForProgress(viewLifecycleOwner)
        binder.nominationPoolsClaimRewardsConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        setupFeeLoading(viewModel.feeLoaderMixin, binder.nominationPoolsClaimRewardsExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(binder.nominationPoolsClaimRewardsConfirm::setProgressState)

        viewModel.pendingRewardsAmountModel.observe(binder.nominationPoolsClaimRewardsAmount::setAmount)

        viewModel.walletUiFlow.observe(binder.nominationPoolsClaimRewardsExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.nominationPoolsClaimRewardsExtrinsicInformation::setAccount)

        binder.nominationPoolsClaimRewardRestakeSwitch.field.bindTo(viewModel.shouldRestakeInput, viewLifecycleOwner.lifecycleScope)
    }
}
