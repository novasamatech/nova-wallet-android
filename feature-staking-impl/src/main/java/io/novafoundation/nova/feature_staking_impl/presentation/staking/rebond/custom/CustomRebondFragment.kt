package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentRebondCustomBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class CustomRebondFragment : BaseFragment<CustomRebondViewModel, FragmentRebondCustomBinding>() {

    override fun createBinding() = FragmentRebondCustomBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.rebondToolbar.applyStatusBarInsets()

        binder.rebondToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.rebondContinue.prepareForProgress(viewLifecycleOwner)
        binder.rebondContinue.setOnClickListener { viewModel.confirmClicked() }
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
        observeHints(viewModel.hintsMixin, binder.rebondHints)
        setupAmountChooser(viewModel.amountChooserMixin, binder.rebondAmount)
        setupFeeLoading(viewModel, binder.rebondFee)

        viewModel.transferableFlow.observe(binder.rebondTransferable::showAmount)

        viewModel.showNextProgress.observe(binder.rebondContinue::setProgressState)
    }
}
