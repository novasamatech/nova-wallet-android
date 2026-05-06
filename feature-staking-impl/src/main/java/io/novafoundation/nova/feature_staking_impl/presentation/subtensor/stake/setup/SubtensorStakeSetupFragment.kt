package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorStakeSetupBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorStakeSetupFragment : BaseFragment<SubtensorStakeSetupViewModel, FragmentSubtensorStakeSetupBinding>() {

    companion object {
        private const val ARG_NETUID = "netuid"
        private const val ARG_SUBNET_NAME = "subnetName"

        /**
         * Saved-state-handle key the validator picker uses to report its
         * selection back to this screen. Picker writes the SS58 hotkey;
         * the ViewModel observes via [StakingRouter.subtensorSelectedValidatorFlow].
         */
        const val KEY_SELECTED_VALIDATOR_HOTKEY = "subtensor_setup_selected_hotkey"

        fun bundle(netuid: Int, subnetName: String? = null): Bundle = Bundle().apply {
            putInt(ARG_NETUID, netuid)
            if (subnetName != null) putString(ARG_SUBNET_NAME, subnetName)
        }
    }

    override fun createBinding() = FragmentSubtensorStakeSetupBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorStakeSetupToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorStakeSetupValidator.setOnClickListener { viewModel.selectValidatorClicked() }
        binder.subtensorStakeSetupAmount.doAfterTextChanged { viewModel.amountChanged(it?.toString().orEmpty()) }
        binder.subtensorStakeSetupContinue.prepareForProgress(viewLifecycleOwner)
        binder.subtensorStakeSetupContinue.setOnClickListener { viewModel.continueClicked() }
    }

    override fun inject() {
        val netuid = arguments?.getInt(ARG_NETUID) ?: 0
        val subnetName = arguments?.getString(ARG_SUBNET_NAME)
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorStakeSetupFactory()
            .create(this, netuid, subnetName)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorStakeSetupViewModel) {
        viewModel.canContinue.observe { enabled ->
            binder.subtensorStakeSetupContinue.isEnabled = enabled
        }
        viewModel.titleText.observe { binder.subtensorStakeSetupToolbar.setTitle(it) }
        viewModel.selectedValidatorLabel.observe { label ->
            binder.subtensorStakeSetupValidatorLabel.text = label
        }
        viewModel.submitting.observe { submitting ->
            binder.subtensorStakeSetupContinue.setProgressState(submitting)
        }
        viewModel.toastEvents.observeEvent { message ->
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
