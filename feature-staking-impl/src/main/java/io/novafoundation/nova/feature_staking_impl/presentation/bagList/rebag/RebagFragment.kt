package io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_rebag.rebagConfirm
import kotlinx.android.synthetic.main.fragment_rebag.rebagCurrentBag
import kotlinx.android.synthetic.main.fragment_rebag.rebagExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_rebag.rebagHints
import kotlinx.android.synthetic.main.fragment_rebag.rebagNewBag
import kotlinx.android.synthetic.main.fragment_rebag.rebagToolbar

class RebagFragment : BaseFragment<RebagViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_rebag, container, false)
    }

    override fun initViews() {
        rebagToolbar.setHomeButtonListener { viewModel.backClicked() }

        rebagConfirm.prepareForProgress(viewLifecycleOwner)
        rebagConfirm.setOnClickListener { viewModel.confirmClicked() }

        rebagExtrinsicInfo.setOnAccountClickedListener { viewModel.accountClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .rebagComponentFractory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: RebagViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, rebagHints)

        setupFeeLoading(viewModel, rebagExtrinsicInfo.fee)
        viewModel.originAddressModelFlow.observe(rebagExtrinsicInfo::setAccount)
        viewModel.walletModel.observe(rebagExtrinsicInfo::setWallet)

        viewModel.rebagMovementModel.observe {
            rebagCurrentBag.showValue(it.currentBag)
            rebagNewBag.showValue(it.newBag)
        }

        viewModel.showNextProgress.observe(rebagConfirm::setProgress)
    }
}
