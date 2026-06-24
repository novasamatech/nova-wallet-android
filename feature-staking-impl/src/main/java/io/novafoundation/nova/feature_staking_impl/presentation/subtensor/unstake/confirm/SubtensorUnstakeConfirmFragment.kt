package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm

import android.os.Bundle
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorUnstakeConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorUnstakeConfirmFragment : BaseFragment<SubtensorUnstakeConfirmViewModel, FragmentSubtensorUnstakeConfirmBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "subtensor_unstake_confirm_payload"

        fun bundle(payload: SubtensorUnstakeConfirmPayload): Bundle = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentSubtensorUnstakeConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorUnstakeConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorUnstakeConfirmExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }
        binder.subtensorUnstakeConfirmValidator.setOnClickListener { viewModel.validatorClicked() }
        binder.subtensorUnstakeConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.subtensorUnstakeConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        // Nova-fee disclosure — visible only when the fee applies (subnet +
        // recipient set). Inert today (recipient null → hidden). The caption is
        // static; the fee row's value is observed in subscribe() once reserves
        // arrive.
        binder.subtensorUnstakeConfirmNovaFee.setVisible(viewModel.novaFeeApplies)
        binder.subtensorUnstakeConfirmNovaFeeDisclaimer.setVisible(viewModel.novaFeeApplies)
        binder.subtensorUnstakeConfirmNovaFeeDisclaimer.text = getString(
            io.novafoundation.nova.feature_staking_impl.R.string.subtensor_setup_nova_fee_disclaimer,
            io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants.NOVA_FEE_PERCENT_DISPLAY,
        )
    }

    override fun inject() {
        val payload = argument<SubtensorUnstakeConfirmPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorUnstakeConfirmFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorUnstakeConfirmViewModel) {
        setupExternalActions(viewModel)

        viewModel.titleText.observe { binder.subtensorUnstakeConfirmToolbar.setTitle(it) }
        viewModel.amountModelFlow.observe(binder.subtensorUnstakeConfirmAmount::setAmount)
        viewModel.walletUiFlow.observe(binder.subtensorUnstakeConfirmExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.subtensorUnstakeConfirmExtrinsicInformation::setAccount)
        viewModel.feeStatusFlow.observe(binder.subtensorUnstakeConfirmExtrinsicInformation::setFeeStatus)
        viewModel.novaFeeStatusFlow.observe(binder.subtensorUnstakeConfirmNovaFee::setFeeStatus)
        viewModel.validatorLabel.observe { binder.subtensorUnstakeConfirmValidator.showValue(it) }
        viewModel.expectedReceiveLabel.observe { receive ->
            // Subnet only — the row is hidden on root since 1 alpha = 1 TAO.
            binder.subtensorUnstakeConfirmReceive.isVisible = receive != null
            if (receive != null) {
                binder.subtensorUnstakeConfirmReceive.showValue(receive)
            }
        }
        viewModel.submitting.observe { binder.subtensorUnstakeConfirmConfirm.setProgressState(it) }
        viewModel.toastEvents.observeEvent { msg ->
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
