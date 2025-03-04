package io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_mythos_redeem.mythosRedeemAmount
import kotlinx.android.synthetic.main.fragment_mythos_redeem.mythosRedeemConfirm
import kotlinx.android.synthetic.main.fragment_mythos_redeem.mythosRedeemContainer
import kotlinx.android.synthetic.main.fragment_mythos_redeem.mythosRedeemExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_mythos_redeem.mythosRedeemToolbar

class MythosRedeemFragment : BaseFragment<MythosRedeemViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_mythos_redeem, container, false)
    }

    override fun initViews() {
        mythosRedeemContainer.applyStatusBarInsets()

        mythosRedeemToolbar.setHomeButtonListener { viewModel.backClicked() }

        mythosRedeemExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        mythosRedeemConfirm.prepareForProgress(viewLifecycleOwner)
        mythosRedeemConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        setupFeeLoading(viewModel.originFeeMixin, mythosRedeemExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(mythosRedeemConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(mythosRedeemExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(mythosRedeemExtrinsicInfo::setWallet)

        viewModel.redeemableAmountModelFlow.observe(mythosRedeemAmount::showLoadingState)
    }
}
