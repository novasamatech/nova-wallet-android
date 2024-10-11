package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmStartParachainStakingFragment : BaseFragment<ConfirmStartParachainStakingViewModel>() {

    companion object {

        private const val PAYLOAD = "ConfirmStartParachainStakingFragment.Payload"

        fun getBundle(payload: ConfirmStartParachainStakingPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_start_confirm, container, false)
    }

    override fun initViews() {
        confirmStartParachainStakingContainer.applyStatusBarInsets()

        confirmStartParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        confirmStartParachainStakingExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        confirmStartParachainStakingConfirm.prepareForProgress(viewLifecycleOwner)
        confirmStartParachainStakingConfirm.setOnClickListener { viewModel.confirmClicked() }

        confirmStartParachainStakingCollator.setOnClickListener { viewModel.collatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmStartParachainStakingFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmStartParachainStakingViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, confirmStartParachainStakingExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, confirmStartParachainStakingHints)

        viewModel.title.observe(confirmStartParachainStakingToolbar::setTitle)
        viewModel.showNextProgress.observe(confirmStartParachainStakingConfirm::setProgressState)

        viewModel.amountModel.observe { amountModel ->

            confirmStartParachainStakingAmount.setAmount(amountModel)
            confirmStartParachainStakingAmount.makeVisible()
        }

        viewModel.currentAccountModelFlow.observe(confirmStartParachainStakingExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(confirmStartParachainStakingExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(confirmStartParachainStakingCollator::showAddress)
        viewModel.amountModel.observe(confirmStartParachainStakingAmount::setAmount)
    }
}
