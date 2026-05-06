package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorUnstakeSetupBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
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
        binder.subtensorUnstakeSetupAmount.doAfterTextChanged { viewModel.amountChanged(it?.toString().orEmpty()) }
        binder.subtensorUnstakeSetupMax.setOnClickListener { viewModel.maxClicked() }
        binder.subtensorUnstakeSetupContinue.prepareForProgress(viewLifecycleOwner)
        binder.subtensorUnstakeSetupContinue.setOnClickListener { viewModel.continueClicked() }
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
        viewModel.titleText.observe { binder.subtensorUnstakeSetupToolbar.setTitle(it) }
        viewModel.validatorLabel.observe { binder.subtensorUnstakeSetupValidator.text = it }
        viewModel.positionLabel.observe { binder.subtensorUnstakeSetupPosition.text = it }
        viewModel.amount.observe { current ->
            // Only push back into the EditText when programmatic — Max button.
            // Avoid loops when the user is typing.
            val edt = binder.subtensorUnstakeSetupAmount
            if (edt.text?.toString().orEmpty() != current) {
                edt.setText(current)
                edt.setSelection(current.length)
            }
        }
        viewModel.canContinue.observe { binder.subtensorUnstakeSetupContinue.isEnabled = it }
        viewModel.submitting.observe { binder.subtensorUnstakeSetupContinue.setProgressState(it) }
        viewModel.toastEvents.observeEvent { msg ->
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
