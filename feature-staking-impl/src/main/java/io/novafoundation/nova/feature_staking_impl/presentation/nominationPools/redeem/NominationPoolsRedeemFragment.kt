package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentNominationPoolsRedeemBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class NominationPoolsRedeemFragment : BaseFragment<NominationPoolsRedeemViewModel, FragmentNominationPoolsRedeemBinding>() {

    override val binder by viewBinding(FragmentNominationPoolsRedeemBinding::bind)

    override fun initViews() {
        binder.nominationPoolsRedeemToolbar.applyStatusBarInsets()

        binder.nominationPoolsRedeemExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.nominationPoolsRedeemToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.nominationPoolsRedeemConfirm.prepareForProgress(viewLifecycleOwner)
        binder.nominationPoolsRedeemConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .nominationPoolsStakingRedeem()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NominationPoolsRedeemViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeLoaderMixin, binder.nominationPoolsRedeemExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(binder.nominationPoolsRedeemConfirm::setProgressState)

        viewModel.redeemAmountModel.observe(binder.nominationPoolsRedeemAmount::setAmount)

        viewModel.walletUiFlow.observe(binder.nominationPoolsRedeemExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.nominationPoolsRedeemExtrinsicInformation::setAccount)
    }
}
