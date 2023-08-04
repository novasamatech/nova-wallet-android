package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup

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
import kotlinx.android.synthetic.main.fragment_nomination_pools_bond_more.nominationPoolsBondMoreAmount
import kotlinx.android.synthetic.main.fragment_nomination_pools_bond_more.nominationPoolsBondMoreContainer
import kotlinx.android.synthetic.main.fragment_nomination_pools_bond_more.nominationPoolsBondMoreContinue
import kotlinx.android.synthetic.main.fragment_nomination_pools_bond_more.nominationPoolsBondMoreFee
import kotlinx.android.synthetic.main.fragment_nomination_pools_bond_more.nominationPoolsBondMoreHints
import kotlinx.android.synthetic.main.fragment_nomination_pools_bond_more.nominationPoolsBondMoreToolbar

class NominationPoolsSetupBondMoreFragment : BaseFragment<NominationPoolsSetupBondMoreViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nomination_pools_bond_more, container, false)
    }

    override fun initViews() {
        nominationPoolsBondMoreContainer.applyStatusBarInsets()

        nominationPoolsBondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        nominationPoolsBondMoreContinue.prepareForProgress(viewLifecycleOwner)
        nominationPoolsBondMoreContinue.setOnClickListener { viewModel.nextClicked() }
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
        setupAmountChooser(viewModel.amountChooserMixin, nominationPoolsBondMoreAmount)
        setupFeeLoading(viewModel, nominationPoolsBondMoreFee)
        observeHints(viewModel.hintsMixin, nominationPoolsBondMoreHints)

        viewModel.buttonState.observe(nominationPoolsBondMoreContinue::setState)
    }
}
