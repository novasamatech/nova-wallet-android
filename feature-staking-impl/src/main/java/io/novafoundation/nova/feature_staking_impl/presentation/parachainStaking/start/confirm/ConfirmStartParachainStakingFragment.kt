package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm

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
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingStartConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmStartParachainStakingFragment : BaseFragment<ConfirmStartParachainStakingViewModel, FragmentParachainStakingStartConfirmBinding>() {

    companion object {

        private const val PAYLOAD = "ConfirmStartParachainStakingFragment.Payload"

        fun getBundle(payload: ConfirmStartParachainStakingPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentParachainStakingStartConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmStartParachainStakingContainer.applyStatusBarInsets()

        binder.confirmStartParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.confirmStartParachainStakingExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmStartParachainStakingConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmStartParachainStakingConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.confirmStartParachainStakingCollator.setOnClickListener { viewModel.collatorClicked() }
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
        setupFeeLoading(viewModel, binder.confirmStartParachainStakingExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, binder.confirmStartParachainStakingHints)

        viewModel.title.observe(binder.confirmStartParachainStakingToolbar::setTitle)
        viewModel.showNextProgress.observe(binder.confirmStartParachainStakingConfirm::setProgressState)

        viewModel.amountModel.observe { amountModel ->
            binder.confirmStartParachainStakingAmount.setAmount(amountModel)
            binder.confirmStartParachainStakingAmount.makeVisible()
        }

        viewModel.currentAccountModelFlow.observe(binder.confirmStartParachainStakingExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.confirmStartParachainStakingExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(binder.confirmStartParachainStakingCollator::showAddress)
        viewModel.amountModel.observe(binder.confirmStartParachainStakingAmount::setAmount)
    }
}
