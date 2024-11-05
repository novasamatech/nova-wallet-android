package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm

import android.os.Bundle

import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmUnbondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmUnbondFragment : BaseFragment<ConfirmUnbondViewModel, FragmentConfirmUnbondBinding>() {

    companion object {

        fun getBundle(payload: ConfirmUnbondPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentConfirmUnbondBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmUnbondToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        binder.confirmUnbondExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.confirmUnbondConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmUnbondConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmUnbondPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmUnbondFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmUnbondViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.confirmUnbondHints)

        viewModel.amountModelFlow.observe(binder.confirmUnbondAmount::setAmount)

        viewModel.showNextProgress.observe(binder.confirmUnbondConfirm::setProgressState)

        viewModel.walletUiFlow.observe(binder.confirmUnbondExtrinsicInformation::setWallet)
        viewModel.feeStatusLiveData.observe(binder.confirmUnbondExtrinsicInformation::setFeeStatus)
        viewModel.originAddressModelFlow.observe(binder.confirmUnbondExtrinsicInformation::setAccount)
    }
}
