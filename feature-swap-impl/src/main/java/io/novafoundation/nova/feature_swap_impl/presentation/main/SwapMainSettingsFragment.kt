package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle

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
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.setupSwapAmountInput
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentMainSwapSettingsBinding
import io.novafoundation.nova.feature_swap_impl.presentation.main.view.GetAssetInBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading

class SwapMainSettingsFragment : BaseFragment<SwapMainSettingsViewModel, FragmentMainSwapSettingsBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "SwapMainSettingsFragment.payload"

        fun getBundle(payload: SwapSettingsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentMainSwapSettingsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.swapMainSettingsToolbar.applyStatusBarInsets()
        binder.swapMainSettingsContinue.prepareForProgress(this)
        binder.swapMainSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.swapMainSettingsToolbar.setRightActionClickListener { viewModel.openOptions() }

        binder.swapMainSettingsPayInput.setSelectTokenClickListener { viewModel.selectPayToken() }
        binder.swapMainSettingsReceiveInput.setSelectTokenClickListener { viewModel.selectReceiveToken() }
        binder.swapMainSettingsFlip.setOnClickListener {
            viewModel.flipAssets()
        }
        binder.swapMainSettingsDetailsRate.setOnClickListener { viewModel.rateDetailsClicked() }
        binder.swapMainSettingsDetailsNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        binder.swapMainSettingsContinue.setOnClickListener { viewModel.continueButtonClicked() }
        binder.swapMainSettingsContinue.prepareForProgress(this)
        binder.swapMainSettingsRoute.setOnClickListener {
            viewModel.routeClicked()

            hideKeyboard()
        }

        binder.swapMainSettingsGetAssetIn.setOnClickListener { viewModel.getAssetInClicked() }
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
        setupSwapAmountInput(viewModel.amountInInput, binder.swapMainSettingsPayInput, binder.swapMainSettingsMaxAmount)
        setupSwapAmountInput(viewModel.amountOutInput, binder.swapMainSettingsReceiveInput, maxAvailableView = null)

        viewModel.feeMixin.setupFeeLoading(binder.swapMainSettingsDetailsNetworkFee)

        viewModel.rateDetails.observe { binder.swapMainSettingsDetailsRate.showLoadingValue(it) }
        viewModel.swapRouteState.observe(binder.swapMainSettingsRoute::setSwapRouteState)
        viewModel.swapExecutionTime.observe(binder.swapMainSettingsExecutionTime::showLoadingValue)
        viewModel.showDetails.observe { binder.swapMainSettingsDetails.setVisible(it) }
        viewModel.buttonState.observe(binder.swapMainSettingsContinue::setState)

        viewModel.swapDirectionFlipped.observeEvent {
            postToUiThread {
                val field = when (it) {
                    SwapDirection.SPECIFIED_IN -> binder.swapMainSettingsPayInput
                    SwapDirection.SPECIFIED_OUT -> binder.swapMainSettingsReceiveInput
                }

                field.requestFocus()
                field.amountInput.setSelectionEnd()
            }
        }

        viewModel.validationProgress.observe(binder.swapMainSettingsContinue::setProgressState)

        viewModel.getAssetInOptionsButtonState.observe(binder.swapMainSettingsGetAssetIn::setState)

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
