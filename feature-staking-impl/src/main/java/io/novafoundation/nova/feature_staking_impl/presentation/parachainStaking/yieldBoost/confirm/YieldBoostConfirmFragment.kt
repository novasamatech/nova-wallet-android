package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentYieldBoostConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmountOrHide

class YieldBoostConfirmFragment : BaseFragment<YieldBoostConfirmViewModel, FragmentYieldBoostConfirmBinding>() {

    companion object {

        private const val PAYLOAD = "YieldBoostConfirmFragment.Payload"

        fun getBundle(payload: YieldBoostConfirmPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentYieldBoostConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmYieldBoostToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.confirmYieldBoostExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmYieldBoostConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmYieldBoostConfirm.setOnClickListener { viewModel.confirmClicked() }
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
        setupFeeLoading(viewModel, binder.confirmYieldBoostExtrinsicInfo.fee)

        viewModel.buttonState.observe(binder.confirmYieldBoostConfirm::setState)

        viewModel.currentAccountModelFlow.observe(binder.confirmYieldBoostExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.confirmYieldBoostExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(binder.confirmYieldBoostCollator::showAddress)

        viewModel.yieldBoostConfigurationUi.observe {
            binder.confirmYieldBoostThreshold.showAmountOrHide(it.threshold)
            binder.confirmYieldBoostFrequency.showValueOrHide(it.frequency)
            binder.confirmYieldBoostMode.showValue(it.mode)
            binder.confirmYieldBoostTerms.text = it.termsText
        }

        binder.confirmYieldBoostTerms.bindTo(viewModel.termsCheckedFlow, viewLifecycleOwner.lifecycleScope)
    }
}
