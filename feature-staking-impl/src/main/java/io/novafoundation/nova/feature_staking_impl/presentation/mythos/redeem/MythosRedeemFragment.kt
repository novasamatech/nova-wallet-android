package io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentMythosRedeemBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class MythosRedeemFragment : BaseFragment<MythosRedeemViewModel, FragmentMythosRedeemBinding>() {

    override fun createBinding() = FragmentMythosRedeemBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.mythosRedeemContainer.applyStatusBarInsets()

        binder.mythosRedeemToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.mythosRedeemExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.mythosRedeemConfirm.prepareForProgress(viewLifecycleOwner)
        binder.mythosRedeemConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .redeemMythosFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MythosRedeemViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.originFeeMixin, binder.mythosRedeemExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(binder.mythosRedeemConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.mythosRedeemExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.mythosRedeemExtrinsicInfo::setWallet)

        viewModel.redeemableAmountModelFlow.observe(binder.mythosRedeemAmount::showLoadingState)
    }
}
