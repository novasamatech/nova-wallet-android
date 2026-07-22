package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm

import android.os.Bundle
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorStakeConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorStakeConfirmFragment : BaseFragment<SubtensorStakeConfirmViewModel, FragmentSubtensorStakeConfirmBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "subtensor_stake_confirm_payload"

        fun bundle(payload: SubtensorStakeConfirmPayload): Bundle = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun createBinding() = FragmentSubtensorStakeConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorStakeConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorStakeConfirmExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }
        binder.subtensorStakeConfirmValidator.setOnClickListener { viewModel.validatorClicked() }
        binder.subtensorStakeConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.subtensorStakeConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        // Nova-fee disclosure — visible only when the fee applies (subnet +
        // recipient set). Inert today (recipient null → hidden). The caption is
        // static; the fee row's value is observed in subscribe().
        binder.subtensorStakeConfirmNovaFee.setVisible(viewModel.novaFeeApplies)
        binder.subtensorStakeConfirmNovaFeeDisclaimer.setVisible(viewModel.novaFeeApplies)
        binder.subtensorStakeConfirmNovaFeeDisclaimer.text = getString(
            io.novafoundation.nova.feature_staking_impl.R.string.subtensor_setup_nova_fee_disclaimer,
            io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants.NOVA_FEE_PERCENT_DISPLAY,
        )
    }

    override fun inject() {
        val payload = argument<SubtensorStakeConfirmPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorStakeConfirmFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorStakeConfirmViewModel) {
        setupExternalActions(viewModel)

        viewModel.titleText.observe { binder.subtensorStakeConfirmToolbar.setTitle(it) }
        viewModel.amountModelFlow.observe(binder.subtensorStakeConfirmAmount::setAmount)
        viewModel.walletUiFlow.observe(binder.subtensorStakeConfirmExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.subtensorStakeConfirmExtrinsicInformation::setAccount)
        viewModel.feeStatusFlow.observe(binder.subtensorStakeConfirmExtrinsicInformation::setFeeStatus)
        viewModel.novaFeeStatusFlow.observe(binder.subtensorStakeConfirmNovaFee::setFeeStatus)
        viewModel.validatorLabel.observe { binder.subtensorStakeConfirmValidator.showValue(it) }
        viewModel.submitting.observe { binder.subtensorStakeConfirmConfirm.setProgressState(it) }
        viewModel.toastEvents.observeEvent { msg ->
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
