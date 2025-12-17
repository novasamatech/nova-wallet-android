package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentConfirmBondMoreBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ConfirmBondMoreFragment : BaseFragment<ConfirmBondMoreViewModel, FragmentConfirmBondMoreBinding>() {

    companion object {

        fun getBundle(payload: ConfirmBondMorePayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentConfirmBondMoreBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.confirmBondMoreExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.confirmBondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.confirmBondMoreConfirm.prepareForProgress(viewLifecycleOwner)
        binder.confirmBondMoreConfirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ConfirmBondMorePayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmBondMoreFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmBondMoreViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.confirmBondMoreHints)

        viewModel.showNextProgress.observe(binder.confirmBondMoreConfirm::setProgressState)

        viewModel.amountModelFlow.observe(binder.confirmBondMoreAmount::setAmount)

        viewModel.feeStatusFlow.observe(binder.confirmBondMoreExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(binder.confirmBondMoreExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.confirmBondMoreExtrinsicInformation::setAccount)
    }
}
