package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentNominationPoolsBondMoreBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class NominationPoolsSetupBondMoreFragment : BaseFragment<NominationPoolsSetupBondMoreViewModel, FragmentNominationPoolsBondMoreBinding>() {

    override val binder by viewBinding(FragmentNominationPoolsBondMoreBinding::bind)

    override fun initViews() {
        binder.nominationPoolsBondMoreContainer.applyStatusBarInsets()

        binder.nominationPoolsBondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.nominationPoolsBondMoreContinue.prepareForProgress(viewLifecycleOwner)
        binder.nominationPoolsBondMoreContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .nominationPoolsStakingSetupBondMore()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NominationPoolsSetupBondMoreViewModel) {
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.nominationPoolsBondMoreAmount)
        setupFeeLoading(viewModel.originFeeMixin, binder.nominationPoolsBondMoreFee)
        observeHints(viewModel.hintsMixin, binder.nominationPoolsBondMoreHints)

        viewModel.buttonState.observe(binder.nominationPoolsBondMoreContinue::setState)
    }
}
