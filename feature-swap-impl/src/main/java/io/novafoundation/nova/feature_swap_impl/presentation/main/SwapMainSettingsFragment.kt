package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.common.utils.setSelectionEnd
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showLoadingValue
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.setupSwapAmountInput
import io.novafoundation.nova.feature_swap_impl.presentation.main.view.GetAssetInBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsContinue
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsDetails
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsDetailsNetworkFee
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsDetailsRate
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsExecutionTime
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsFlip
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsGetAssetIn
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsMaxAmount
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsPayInput
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsReceiveInput
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsRoute
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsToolbar
import javax.inject.Inject

class SwapMainSettingsFragment : BaseFragment<SwapMainSettingsViewModel>() {

    companion object {

        private const val KEY_PAYLOAD = "SwapMainSettingsFragment.payload"

        fun getBundle(payload: SwapSettingsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_main_swap_settings, container, false)
    }

    override fun initViews() {
        swapMainSettingsToolbar.applyStatusBarInsets()
        swapMainSettingsContinue.prepareForProgress(this)
        swapMainSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        swapMainSettingsToolbar.setRightActionClickListener { viewModel.openOptions() }

        swapMainSettingsPayInput.setSelectTokenClickListener { viewModel.selectPayToken() }
        swapMainSettingsReceiveInput.setSelectTokenClickListener { viewModel.selectReceiveToken() }
        swapMainSettingsFlip.setOnClickListener {
            viewModel.flipAssets()
        }
        swapMainSettingsDetailsRate.setOnClickListener { viewModel.rateDetailsClicked() }
        swapMainSettingsDetailsNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        swapMainSettingsContinue.setOnClickListener { viewModel.continueButtonClicked() }
        swapMainSettingsContinue.prepareForProgress(this)
        swapMainSettingsRoute.setOnClickListener {
            viewModel.routeClicked()

            hideKeyboard()
        }

        swapMainSettingsGetAssetIn.setOnClickListener { viewModel.getAssetInClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapMainSettings()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SwapMainSettingsViewModel) {
        observeDescription(viewModel)
        observeValidations(viewModel)
        setupSwapAmountInput(viewModel.amountInInput, swapMainSettingsPayInput, swapMainSettingsMaxAmount)
        setupSwapAmountInput(viewModel.amountOutInput, swapMainSettingsReceiveInput, maxAvailableView = null)

        viewModel.feeMixin.setupFeeLoading(swapMainSettingsDetailsNetworkFee)

        viewModel.rateDetails.observe { swapMainSettingsDetailsRate.showLoadingValue(it) }
        viewModel.swapRouteState.observe(swapMainSettingsRoute::setSwapRouteState)
        viewModel.swapExecutionTime.observe(swapMainSettingsExecutionTime::showLoadingValue)
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

        viewModel.validationProgress.observe(swapMainSettingsContinue::setProgressState)

        viewModel.getAssetInOptionsButtonState.observe(swapMainSettingsGetAssetIn::setState)

        viewModel.selectGetAssetInOption.awaitableActionLiveData.observeEvent {
            GetAssetInBottomSheet(
                context = requireContext(),
                onCancel = it.onCancel,
                payload = it.payload,
                onClicked = it.onSuccess
            ).show()
        }
    }
}
