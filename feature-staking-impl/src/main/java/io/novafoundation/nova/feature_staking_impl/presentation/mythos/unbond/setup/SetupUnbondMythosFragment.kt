package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup

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
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondAmountField
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondCollator
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondContainer
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondFee
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondNext
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondToolbar
import kotlinx.android.synthetic.main.fragment_mythos_unbond.mythosUnbondTransferable

class SetupUnbondMythosFragment : BaseFragment<SetupUnbondMythosViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_mythos_unbond, container, false)
    }

    override fun initViews() {
        mythosUnbondContainer.applyStatusBarInsets()

        mythosUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        mythosUnbondNext.prepareForProgress(viewLifecycleOwner)
        mythosUnbondNext.setOnClickListener { viewModel.nextClicked() }

        mythosUnbondCollator.setOnClickListener { viewModel.selectCollatorClicked() }
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
        observeRetries(viewModel)
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, mythosUnbondAmountField)
        setupFeeLoading(viewModel, mythosUnbondFee)

        viewModel.selectedCollatorModel.observe(mythosUnbondCollator::setSelectedTarget)

        viewModel.buttonState.observe(mythosUnbondNext::setState)

        viewModel.transferable.observe(mythosUnbondTransferable::showAmount)

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
