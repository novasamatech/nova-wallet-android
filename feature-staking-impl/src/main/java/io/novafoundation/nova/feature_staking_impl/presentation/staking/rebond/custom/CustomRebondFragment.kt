package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondAmount
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondContinue
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondFee
import kotlinx.android.synthetic.main.fragment_rebond_custom.rebondToolbar

class CustomRebondFragment : BaseFragment<CustomRebondViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_rebond_custom, container, false)
    }

    override fun initViews() {
        rebondToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

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
        observeRetries(viewModel)

        viewModel.showNextProgress.observe(rebondContinue::setProgress)

        viewModel.assetModelFlow.observe {
            rebondAmount.setAssetBalance(it.assetBalance)
            rebondAmount.setAssetName(it.tokenName)
            rebondAmount.loadAssetImage(it.imageUrl)
        }

        rebondAmount.amountInput.bindTo(viewModel.enteredAmountFlow, lifecycleScope)

        viewModel.amountFiatFLow.observe {
            it.let(rebondAmount::setFiatAmount)
        }

        viewModel.feeLiveData.observe(rebondFee::setFeeStatus)
    }
}
