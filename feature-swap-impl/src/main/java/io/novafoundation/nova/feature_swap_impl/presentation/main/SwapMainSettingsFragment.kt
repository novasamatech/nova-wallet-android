package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.common.utils.setSelectionEnd
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showLoadingValue
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.setupSwapAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsContinue
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsDetails
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsDetailsNetworkFee
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsDetailsRate
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsFlip
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsMaxAmount
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsMaxAmountButton
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsPayInput
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsReceiveInput
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsToolbar

class SwapMainSettingsFragment : BaseFragment<SwapMainSettingsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_main_swap_settings, container, false)
    }

    override fun initViews() {
        swapMainSettingsToolbar.applyStatusBarInsets()
        swapMainSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }

        swapMainSettingsMaxAmountButton.setOnClickListener { viewModel.maxTokens() }
        swapMainSettingsPayInput.setOnClickListener { viewModel.selectPayToken() }
        swapMainSettingsReceiveInput.setOnClickListener { viewModel.selectReceiveToken() }
        swapMainSettingsFlip.setOnClickListener {
            viewModel.flipAssets()
        }
        swapMainSettingsDetailsRate.setOnClickListener { viewModel.rateDetailsClicked() }
        swapMainSettingsDetailsNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        swapMainSettingsContinue.setOnClickListener { viewModel.confirmButtonClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapMainSettings()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapMainSettingsViewModel) {
        setupSwapAmountInput(viewModel.amountInInput, swapMainSettingsPayInput)
        setupSwapAmountInput(viewModel.amountOutInput, swapMainSettingsReceiveInput)
        setupFeeLoading(viewModel.feeMixin, swapMainSettingsDetailsNetworkFee)

        viewModel.amountInInput.maxAvailable.observe {
            swapMainSettingsMaxAmountButton.isGone = it.isNullOrEmpty()
            swapMainSettingsMaxAmount.setTextOrHide(it)
        }

        viewModel.rateDetails.observe { swapMainSettingsDetailsRate.showLoadingValue(it) }
        viewModel.showDetails.observe { swapMainSettingsDetails.setVisible(it) }
        viewModel.buttonState.observe(swapMainSettingsContinue::setState)

        viewModel.swapDirectionFlipped.observeEvent {
            postToUiThread {
                val field = when (it) {
                    SwapDirection.SPECIFIED_IN -> swapMainSettingsPayInput
                    SwapDirection.SPECIFIED_OUT -> swapMainSettingsReceiveInput
                }

                field.requestFocus()
                field.amountInput.setSelectionEnd()
            }
        }
    }
}
