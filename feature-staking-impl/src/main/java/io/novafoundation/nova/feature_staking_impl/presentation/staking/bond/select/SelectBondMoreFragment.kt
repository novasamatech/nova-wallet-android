package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBondMoreBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class SelectBondMoreFragment : BaseFragment<SelectBondMoreViewModel, FragmentBondMoreBinding>() {

    companion object {

        fun getBundle(payload: SelectBondMorePayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override val binder by viewBinding(FragmentBondMoreBinding::bind)

    override fun initViews() {
        binder.bondMoreContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        binder.bondMoreToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.bondMoreContinue.prepareForProgress(viewLifecycleOwner)
        binder.bondMoreContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        val payload = argument<SelectBondMorePayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectBondMoreFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectBondMoreViewModel) {
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.bondMoreAmount)
        setupFeeLoading(viewModel, binder.bondMoreFee)
        observeHints(viewModel.hintsMixin, binder.bondMoreHints)

        viewModel.showNextProgress.observe(binder.bondMoreContinue::setProgressState)

        viewModel.feeLiveData.observe(binder.bondMoreFee::setFeeStatus)
    }
}
