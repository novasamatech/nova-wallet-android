package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentNominationPoolsSetupUnbondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class NominationPoolsSetupUnbondFragment : BaseFragment<NominationPoolsSetupUnbondViewModel, FragmentNominationPoolsSetupUnbondBinding>() {

    override val binder by viewBinding(FragmentNominationPoolsSetupUnbondBinding::bind)

    override fun initViews() {
        binder.nominationPoolsUnbondContainer.applyStatusBarInsets()

        binder.nominationPoolsUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.nominationPoolsUnbondContinue.prepareForProgress(viewLifecycleOwner)
        binder.nominationPoolsUnbondContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .nominationPoolsStakingSetupUnbond()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NominationPoolsSetupUnbondViewModel) {
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.nominationPoolsUnbondAmount)
        setupFeeLoading(viewModel.originFeeMixin, binder.nominationPoolsUnbondFee)
        observeHints(viewModel.hintsMixin, binder.nominationPoolsUnbondHints)

        viewModel.transferableBalance.observe(binder.nominationPoolsUnbondTransferable::showAmount)

        viewModel.buttonState.observe(binder.nominationPoolsUnbondContinue::setState)
    }
}
