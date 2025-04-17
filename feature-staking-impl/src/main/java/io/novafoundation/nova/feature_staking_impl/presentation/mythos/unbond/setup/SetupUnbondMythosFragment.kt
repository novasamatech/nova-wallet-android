package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentMythosUnbondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class SetupUnbondMythosFragment : BaseFragment<SetupUnbondMythosViewModel, FragmentMythosUnbondBinding>() {

    override fun createBinding() = FragmentMythosUnbondBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.mythosUnbondContainer.applyStatusBarInsets()

        binder.mythosUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        binder.mythosUnbondNext.prepareForProgress(viewLifecycleOwner)
        binder.mythosUnbondNext.setOnClickListener { viewModel.nextClicked() }

        binder.mythosUnbondCollator.setOnClickListener { viewModel.selectCollatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .setupUnbondMythosFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SetupUnbondMythosViewModel) {
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.mythosUnbondAmountField)
        setupFeeLoading(viewModel.feeLoaderMixin, binder.mythosUnbondFee)

        viewModel.selectedCollatorModel.observe(binder.mythosUnbondCollator::setSelectedTarget)

        viewModel.buttonState.observe(binder.mythosUnbondNext::setState)

        viewModel.transferable.observe(binder.mythosUnbondTransferable::showAmount)

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
