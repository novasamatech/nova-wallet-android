package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondAmount
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondContinue
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondFee
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondHints
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondToolbar
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondTransferable

class CustomRebondFragment : BaseFragment<CustomRebondViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_rebond_custom, container, false)
    }

    override fun initViews() {
        rebondToolbar.applyStatusBarInsets()

        rebondToolbar.setHomeButtonListener { viewModel.backClicked() }
        rebondContinue.prepareForProgress(viewLifecycleOwner)
        rebondContinue.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .rebondCustomFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CustomRebondViewModel) {
        observeValidations(viewModel)
        observeHints(viewModel.hintsMixin, rebondHints)
        setupAmountChooser(viewModel.amountChooserMixin, rebondAmount)
        setupFeeLoading(viewModel, rebondFee)

        viewModel.transferableFlow.observe(rebondTransferable::showAmount)

        viewModel.showNextProgress.observe(rebondContinue::setProgressState)
    }
}
