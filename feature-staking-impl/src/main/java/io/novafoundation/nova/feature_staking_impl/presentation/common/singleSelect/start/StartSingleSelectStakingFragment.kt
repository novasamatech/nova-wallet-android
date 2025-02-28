package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.address.WithAccountId
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards.setupParachainStakingRewardsComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingAmountField
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingCollator
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingFee
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingHints
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingMinStake
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingNext
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingRewards
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingToolbar

abstract class StartSingleSelectStakingFragment<T, V : StartSingleSelectStakingViewModel<T, *>> : BaseFragment<V>()
    where T : Identifiable, T : WithAccountId {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_parachain_staking_start, container, false)
    }

    override fun initViews() {
        startParachainStakingContainer.applyStatusBarInsets()

        startParachainStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        startParachainStakingNext.prepareForProgress(viewLifecycleOwner)
        startParachainStakingNext.setOnClickListener { viewModel.nextClicked() }

        startParachainStakingCollator.setOnClickListener { viewModel.selectTargetClicked() }
    }

    override fun subscribe(viewModel: V) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, startParachainStakingAmountField)
        setupParachainStakingRewardsComponent(viewModel.rewardsComponent, startParachainStakingRewards)
        setupFeeLoading(viewModel, startParachainStakingFee)
        observeHints(viewModel.hintsMixin, startParachainStakingHints)

        viewModel.title.observe(startParachainStakingToolbar::setTitle)

        viewModel.selectedTargetModelFlow.observe {
            startParachainStakingCollator.setSelectedTarget(it)
        }

        viewModel.buttonState.observe(startParachainStakingNext::setState)

        viewModel.minimumStake.observe(startParachainStakingMinStake::showAmount)

        viewModel.chooseTargetAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedStakeTargetsBottomSheet(
                context = requireContext(),
                payload = action.payload,
                onResponse = action.onSuccess,
                onCancel = action.onCancel,
            ).show()
        }
    }
}
