package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmountOrHide

class YieldBoostConfirmFragment : BaseFragment<YieldBoostConfirmViewModel>() {

    companion object {

        private const val PAYLOAD = "YieldBoostConfirmFragment.Payload"

        fun getBundle(payload: YieldBoostConfirmPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_yield_boost_confirm, container, false)
    }

    override fun initViews() {
        confirmYieldBoostContainer.applyStatusBarInsets()

        confirmYieldBoostToolbar.setHomeButtonListener { viewModel.backClicked() }

        confirmYieldBoostExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        confirmYieldBoostConfirm.prepareForProgress(viewLifecycleOwner)
        confirmYieldBoostConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmYieldBoostComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: YieldBoostConfirmViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, confirmYieldBoostExtrinsicInfo.fee)

        viewModel.buttonState.observe(confirmYieldBoostConfirm::setState)

        viewModel.currentAccountModelFlow.observe(confirmYieldBoostExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(confirmYieldBoostExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(confirmYieldBoostCollator::showAddress)

        viewModel.yieldBoostConfigurationUi.observe {
            confirmYieldBoostThreshold.showAmountOrHide(it.threshold)
            confirmYieldBoostFrequency.showValueOrHide(it.frequency)
            confirmYieldBoostMode.showValue(it.mode)
            confirmYieldBoostTerms.text = it.termsText
        }

        confirmYieldBoostTerms.bindTo(viewModel.termsCheckedFlow, viewLifecycleOwner.lifecycleScope)
    }
}
