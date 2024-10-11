package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class NominationPoolsSetupUnbondFragment : BaseFragment<NominationPoolsSetupUnbondViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nomination_pools_setup_unbond, container, false)
    }

    override fun initViews() {
        nominationPoolsUnbondContainer.applyStatusBarInsets()

        nominationPoolsUnbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        nominationPoolsUnbondContinue.prepareForProgress(viewLifecycleOwner)
        nominationPoolsUnbondContinue.setOnClickListener { viewModel.nextClicked() }
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
        setupAmountChooser(viewModel.amountChooserMixin, nominationPoolsUnbondAmount)
        setupFeeLoading(viewModel.originFeeMixin, nominationPoolsUnbondFee)
        observeHints(viewModel.hintsMixin, nominationPoolsUnbondHints)

        viewModel.transferableBalance.observe(nominationPoolsUnbondTransferable::showAmount)

        viewModel.buttonState.observe(nominationPoolsUnbondContinue::setState)
    }
}
