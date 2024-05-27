package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem

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
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_nomination_pools_redeem.nominationPoolsRedeemAmount
import kotlinx.android.synthetic.main.fragment_nomination_pools_redeem.nominationPoolsRedeemConfirm
import kotlinx.android.synthetic.main.fragment_nomination_pools_redeem.nominationPoolsRedeemExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_nomination_pools_redeem.nominationPoolsRedeemToolbar

class NominationPoolsRedeemFragment : BaseFragment<NominationPoolsRedeemViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_nomination_pools_redeem, container, false)
    }

    override fun initViews() {
        nominationPoolsRedeemToolbar.applyStatusBarInsets()

        nominationPoolsRedeemExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        nominationPoolsRedeemToolbar.setHomeButtonListener { viewModel.backClicked() }
        nominationPoolsRedeemConfirm.prepareForProgress(viewLifecycleOwner)
        nominationPoolsRedeemConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        setupFeeLoading(viewModel.feeLoaderMixin, nominationPoolsRedeemExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(nominationPoolsRedeemConfirm::setProgressState)

        viewModel.redeemAmountModel.observe(nominationPoolsRedeemAmount::setAmount)

        viewModel.walletUiFlow.observe(nominationPoolsRedeemExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(nominationPoolsRedeemExtrinsicInformation::setAccount)
    }
}
