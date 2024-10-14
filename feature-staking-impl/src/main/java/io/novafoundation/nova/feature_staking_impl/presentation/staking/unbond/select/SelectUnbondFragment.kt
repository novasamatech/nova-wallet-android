package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select

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
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSelectUnbondBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class SelectUnbondFragment : BaseFragment<SelectUnbondViewModel,FragmentSelectUnbondBinding>() {

    override val binder by viewBinding(FragmentSelectUnbondBinding::bind)

    override fun initViews() {
        binder.unbondContainer.applyStatusBarInsets()

        binder.unbondToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.unbondContinue.prepareForProgress(viewLifecycleOwner)
        binder.unbondContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectUnbondFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectUnbondViewModel) {
        observeValidations(viewModel)
        setupFeeLoading(viewModel, binder.unbondFee)
        observeHints(viewModel.hintsMixin, binder.unbondHints)
        setupAmountChooser(viewModel.amountMixin, binder.unbondAmount)

        viewModel.transferableFlow.observe(binder.unbondTransferable::showAmount)

        viewModel.showNextProgress.observe(binder.unbondContinue::setProgressState)
    }
}
