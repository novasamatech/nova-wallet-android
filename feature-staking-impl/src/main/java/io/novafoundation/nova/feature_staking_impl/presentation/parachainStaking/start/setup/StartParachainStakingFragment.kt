package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.ChooseCollatorResponse
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.setupParachainStakingRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.view.ChooseStakedCollatorBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingAmountField
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingCollator
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingContainer
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingFee
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingMinStake
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingNext
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingRewards
import kotlinx.android.synthetic.main.fragment_parachain_staking_start.startParachainStakingToolbar

class StartParachainStakingFragment : BaseFragment<StartParachainStakingViewModel>() {

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

        startParachainStakingCollator.setOnClickListener { viewModel.selectCollatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .startParachainStakingFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartParachainStakingViewModel) {
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, startParachainStakingAmountField)
        setupParachainStakingRewardsComponent(viewModel.rewardsComponent, startParachainStakingRewards)
        setupFeeLoading(viewModel, startParachainStakingFee)

        viewModel.title.observe(startParachainStakingToolbar::setTitle)

        viewModel.selectedCollatorModel.observe {
            startParachainStakingCollator.setSelectedCollator(it)
        }

        viewModel.buttonState.observe(startParachainStakingNext::setState)

        viewModel.minimumStake.observe(startParachainStakingMinStake::showAmount)

        viewModel.chooseCollatorAction.awaitableActionLiveData.observeEvent { action ->
            ChooseStakedCollatorBottomSheet(
                context = requireContext(),
                payload = action.payload,
                stakedCollatorSelected = { action.onSuccess(ChooseCollatorResponse.Existing(it)) },
                onCancel = action.onCancel,
                newCollatorClicked = { action.onSuccess(ChooseCollatorResponse.New) }
            ).show()
        }
    }
}
