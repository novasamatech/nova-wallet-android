package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmRebondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmRebondFragment : BaseFragment<ConfirmRebondViewModel, FragmentConfirmRebondBinding>() {

    companion object {

        fun getBundle(payload: ConfirmRebondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentConfirmRebondBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmRebondToolbar.applyStatusBarInsets()

        binder.confirmRebondExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmRebondToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.confirmRebondConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmRebondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmRebondPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmRebondFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmRebondViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.confirmRebondHints)
        setupFeeLoading(viewModel, binder.confirmRebondExtrinsicInformation.fee)

        viewModel.showNextProgress.observe(binder.confirmRebondConfirm::setProgressState)

        viewModel.amountModelFlow.observe(binder.confirmRebondAmount::setAmount)

        viewModel.walletUiFlow.observe(binder.confirmRebondExtrinsicInformation::setWallet)
        viewModel.feeLiveData.observe(binder.confirmRebondExtrinsicInformation::setFeeStatus)
        viewModel.originAddressModelFlow.observe(binder.confirmRebondExtrinsicInformation::setAccount)
    }
}
