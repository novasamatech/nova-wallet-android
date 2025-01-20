package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondAmountField
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondCollator
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondFee
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondHints
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondMinStake
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondNext
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondToolbar
import kotlinx.android.synthetic.main.fragment_parachain_staking_unbond.parachainStakingUnbondTransferable

class ParachainStakingUnbondFragment : BaseFragment<ParachainStakingUnbondViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_unbond, container, false)
    }

    override fun initViews() {
        parachainStakingUnbondContainer.applyStatusBarInsets()

        parachainStakingUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        parachainStakingUnbondNext.prepareForProgress(viewLifecycleOwner)
        parachainStakingUnbondNext.setOnClickListener { viewModel.nextClicked() }

        parachainStakingUnbondCollator.setOnClickListener { viewModel.selectCollatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .parachainStakingUnbondSetupFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ParachainStakingUnbondViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, parachainStakingUnbondAmountField)
        setupFeeLoading(viewModel, parachainStakingUnbondFee)
        observeHints(viewModel.hintsMixin, parachainStakingUnbondHints)

        viewModel.selectedCollatorModel.observe(parachainStakingUnbondCollator::setSelectedTarget)

        viewModel.buttonState.observe(parachainStakingUnbondNext::setState)

        viewModel.minimumStake.observe(parachainStakingUnbondMinStake::showAmount)
        viewModel.transferable.observe(parachainStakingUnbondTransferable::showAmount)

        viewModel.chooseCollatorAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                stakedCollatorSelected = { _, item -> action.onSuccess(item) },
                onCancel = action.onCancel,
                newStakeTargetClicked = null
            ).show()
        }
    }
}
