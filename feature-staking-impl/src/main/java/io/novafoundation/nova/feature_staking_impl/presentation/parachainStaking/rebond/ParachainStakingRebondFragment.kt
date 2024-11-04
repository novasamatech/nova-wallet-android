package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond

import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingRebondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ParachainStakingRebondFragment : BaseFragment<ParachainStakingRebondViewModel, FragmentParachainStakingRebondBinding>() {

    companion object {

        private const val PAYLOAD = "ParachainStakingRebondFragment.Payload"

        fun getBundle(payload: ParachainStakingRebondPayload) = bundleOf(PAYLOAD to payload)
    }

    override val binder by viewBinding(FragmentParachainStakingRebondBinding::bind)

    override fun initViews() {
        binder.parachainStakingRebondContainer.applyStatusBarInsets()

        binder.parachainStakingRebondToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.parachainStakingRebondExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.parachainStakingRebondCollator.setOnClickListener { viewModel.collatorClicked() }

        binder.parachainStakingRebondConfirm.prepareForProgress(viewLifecycleOwner)
        binder.parachainStakingRebondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingRebondFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingRebondViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, binder.parachainStakingRebondExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, binder.parachainStakingRebondHints)

        viewModel.showNextProgress.observe(binder.parachainStakingRebondConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.parachainStakingRebondExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.parachainStakingRebondExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(binder.parachainStakingRebondCollator::showAddress)

        viewModel.rebondAmount.observe(binder.parachainStakingRebondAmount::showLoadingState)
    }
}
