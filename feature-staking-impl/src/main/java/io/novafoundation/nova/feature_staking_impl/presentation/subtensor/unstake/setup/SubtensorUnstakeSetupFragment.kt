package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorUnstakeSetupBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import java.math.BigInteger

class SubtensorUnstakeSetupFragment : BaseFragment<SubtensorUnstakeSetupViewModel, FragmentSubtensorUnstakeSetupBinding>() {

    companion object {
        private const val ARG_NETUID = "unstake_netuid"
        private const val ARG_HOTKEY = "unstake_hotkey"
        private const val ARG_AMOUNT = "unstake_amount"

        fun bundle(netuid: Int, hotkeyAddress: String, positionPlanks: BigInteger): Bundle = Bundle().apply {
            putInt(ARG_NETUID, netuid)
            putString(ARG_HOTKEY, hotkeyAddress)
            putString(ARG_AMOUNT, positionPlanks.toString())
        }
    }

    override fun createBinding() = FragmentSubtensorUnstakeSetupBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorUnstakeSetupToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorUnstakeSetupAmountInput.amountInput.doAfterTextChanged {
            viewModel.amountChanged(it?.toString().orEmpty())
        }
        // MaxAmountView wires the click via setMaxActionAvailability — set
        // it once with our handler. The widget colours itself based on
        // availability (blue when clickable, grey otherwise).
        binder.subtensorUnstakeSetupMax.setMaxActionAvailability(
            MaxActionAvailability.Available { viewModel.maxClicked() }
        )
        binder.subtensorUnstakeSetupContinue.setOnClickListener { viewModel.continueClicked() }

        // Nova-fee disclosure — visible only when the fee applies (subnet +
        // recipient set). Inert today (recipient null → hidden). The caption is
        // static; the fee row's value is observed in subscribe().
        binder.subtensorUnstakeSetupNovaFee.setVisible(viewModel.novaFeeApplies)
        binder.subtensorUnstakeSetupNovaFeeDisclaimer.setVisible(viewModel.novaFeeApplies)
        binder.subtensorUnstakeSetupNovaFeeDisclaimer.text = getString(
            io.novafoundation.nova.feature_staking_impl.R.string.subtensor_setup_nova_fee_disclaimer,
            io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants.NOVA_FEE_PERCENT_DISPLAY,
        )
    }

    override fun inject() {
        val netuid = arguments?.getInt(ARG_NETUID) ?: 0
        val hotkey = arguments?.getString(ARG_HOTKEY).orEmpty()
        val planks = arguments?.getString(ARG_AMOUNT)?.let { runCatching { BigInteger(it) }.getOrNull() } ?: BigInteger.ZERO
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorUnstakeSetupFactory()
            .create(this, netuid, hotkey, planks)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorUnstakeSetupViewModel) {
        // Canonical Nova fee row — same wire-up as bond_more / parachain
        // start. The mixin owns shimmer / loaded / error states.
        setupFeeLoading(viewModel.feeMixin, binder.subtensorUnstakeSetupFee)

        // Nova-fee row — observed independently of the network-fee mixin. Reuses
        // the standard FeeView.setFeeStatus binding; NoFee hides the row.
        viewModel.novaFeeStatusFlow.observe(binder.subtensorUnstakeSetupNovaFee::setFeeStatus)

        viewModel.titleText.observe { binder.subtensorUnstakeSetupToolbar.setTitle(it) }
        viewModel.validatorTargetModel.observe { binder.subtensorUnstakeSetupValidator.setModel(it) }
        viewModel.positionLabel.observe { binder.subtensorUnstakeSetupPosition.showValue(it) }
        viewModel.maxLabel.observe { binder.subtensorUnstakeSetupMax.setMaxAmountDisplay(it) }

        // Static chip — the ticker ("SN56" / "TAO") doesn't change after
        // construction. Set once instead of plumbing a Flow.
        binder.subtensorUnstakeSetupAmountInput.setAssetName(viewModel.ticker)
        viewModel.tokenIcon.observe { icon ->
            icon?.let { binder.subtensorUnstakeSetupAmountInput.loadAssetImage(it) }
        }
        viewModel.fiatAmount.observe { fiat ->
            binder.subtensorUnstakeSetupAmountInput.setFiatAmount(fiat)
        }

        viewModel.amount.observe { current ->
            val edt = binder.subtensorUnstakeSetupAmountInput.amountInput
            if (edt.text?.toString().orEmpty() != current) {
                edt.setText(current)
                edt.setSelection(current.length)
            }
        }
        viewModel.canContinue.observe { binder.subtensorUnstakeSetupContinue.isEnabled = it }
        viewModel.toastEvents.observeEvent { msg ->
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
        viewModel.unstakeAllPromptEvent.observeEvent { showUnstakeAllPrompt() }
    }

    private fun showUnstakeAllPrompt() {
        // Mirrors the iOS relaychain "min stake not crossed" affordance:
        // user is about to leave dust under the chain minimum, offer to
        // unstake everything as a single action instead.
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(io.novafoundation.nova.feature_staking_impl.R.string.subtensor_unstake_remainder_title)
            .setMessage(io.novafoundation.nova.feature_staking_impl.R.string.subtensor_unstake_remainder_message)
            .setPositiveButton(io.novafoundation.nova.feature_staking_impl.R.string.subtensor_unstake_all) { _, _ ->
                viewModel.confirmUnstakeAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
