package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup

import android.os.Bundle
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorStakeSetupBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class SubtensorStakeSetupFragment : BaseFragment<SubtensorStakeSetupViewModel, FragmentSubtensorStakeSetupBinding>() {

    companion object {
        private const val ARG_NETUID = "netuid"
        private const val ARG_SUBNET_NAME = "subnetName"

        /**
         * Saved-state-handle key the validator picker uses to report its
         * selection back to this screen. Picker writes a
         * [SubtensorPickedValidator] (hotkey + identity); the ViewModel
         * observes via [StakingRouter.subtensorSelectedValidatorFlow].
         */
        const val KEY_SELECTED_VALIDATOR = "subtensor_setup_selected_validator"

        fun bundle(netuid: Int, subnetName: String? = null): Bundle = Bundle().apply {
            putInt(ARG_NETUID, netuid)
            if (subnetName != null) putString(ARG_SUBNET_NAME, subnetName)
        }
    }

    override fun createBinding() = FragmentSubtensorStakeSetupBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorStakeSetupToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorStakeSetupValidator.setOnClickListener { viewModel.selectValidatorClicked() }
        binder.subtensorStakeSetupContinue.prepareForProgress(viewLifecycleOwner)
        binder.subtensorStakeSetupContinue.setOnClickListener { viewModel.continueClicked() }

        // Min stake row is static — set once and forget. Mirrors iOS
        // `TitleAmountView.dark()` rendered with `MIN_NOMINATOR_STAKE_RAO`.
        binder.subtensorStakeSetupMinStake.showValue("0.01 TAO")
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
        // Canonical Nova staking-flow wiring — same setup helpers used by
        // bond_more, parachain start, nomination-pools setup, etc. so the
        // amount/fee rows look + behave identically across all flows.
        setupAmountChooser(viewModel.amountChooserMixin, binder.subtensorStakeSetupAmount)
        setupFeeLoading(viewModel.originFeeMixin, binder.subtensorStakeSetupFee)

        viewModel.titleText.observe { binder.subtensorStakeSetupToolbar.setTitle(it) }
        viewModel.validatorTargetModel.observe { binder.subtensorStakeSetupValidator.setModel(it) }
        viewModel.canContinue.observe { binder.subtensorStakeSetupContinue.isEnabled = it }
        viewModel.submitting.observe { binder.subtensorStakeSetupContinue.setProgressState(it) }
        viewModel.toastEvents.observeEvent { msg ->
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
