package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentParachainStakingUnbondConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ParachainStakingUnbondConfirmFragment : BaseFragment<ParachainStakingUnbondConfirmViewModel, FragmentParachainStakingUnbondConfirmBinding>() {

    companion object {

        private const val PAYLOAD = "ParachainStakingUnbondConfirmFragment.Payload"

        fun getBundle(payload: ParachainStakingUnbondConfirmPayload) = bundleOf(PAYLOAD to payload)
    }

    override val binder by viewBinding(FragmentParachainStakingUnbondConfirmBinding::bind)

    override fun initViews() {
        binder.parachainStakingUnbondConfirmContainer.applyStatusBarInsets()

        binder.parachainStakingUnbondConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.parachainStakingUnbondConfirmExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.parachainStakingUnbondConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.parachainStakingUnbondConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.parachainStakingUnbondConfirmCollator.setOnClickListener { viewModel.collatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingUnbondConfirmFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingUnbondConfirmViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, binder.parachainStakingUnbondConfirmExtrinsicInfo.fee)
        observeHints(viewModel.hintsMixin, binder.parachainStakingUnbondConfirmHints)

        viewModel.showNextProgress.observe(binder.parachainStakingUnbondConfirmConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.parachainStakingUnbondConfirmExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.parachainStakingUnbondConfirmExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(binder.parachainStakingUnbondConfirmCollator::showAddress)
        viewModel.amountModel.observe(binder.parachainStakingUnbondConfirmAmount::setAmount)
    }
}
