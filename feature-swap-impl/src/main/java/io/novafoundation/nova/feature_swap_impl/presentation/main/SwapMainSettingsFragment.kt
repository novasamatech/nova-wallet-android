package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.common.utils.setSelectionEnd
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
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsMinBalanceAlert
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsPayInput
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsReceiveInput
import kotlinx.android.synthetic.main.fragment_main_swap_settings.swapMainSettingsToolbar

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
        swapMainSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }
        swapMainSettingsToolbar.setRightActionClickListener { viewModel.openOptions() }

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
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SwapMainSettingsViewModel) {
        setupSwapAmountInput(viewModel.amountInInput, swapMainSettingsPayInput, swapMainSettingsMaxAmount)
        setupSwapAmountInput(viewModel.amountOutInput, swapMainSettingsReceiveInput, maxAvailableView = null)
        setupFeeLoading(viewModel.feeMixin, swapMainSettingsDetailsNetworkFee)

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

        viewModel.minimumBalanceBuyAlert.observe(swapMainSettingsMinBalanceAlert::setModel)

        viewModel.canChangeFeeToken.observe { canChangeFeeToken ->
            if (canChangeFeeToken) {
                swapMainSettingsDetailsNetworkFee.setPrimaryValueStartIcon(R.drawable.ic_pencil_edit)
                swapMainSettingsDetailsNetworkFee.setOnValueClickListener { viewModel.editFeeTokenClicked() }
            } else {
                swapMainSettingsDetailsNetworkFee.setPrimaryValueStartIcon(null)
                swapMainSettingsDetailsNetworkFee.setOnValueClickListener(null)
            }
        }

        viewModel.changeFeeTokenEvent.awaitableActionLiveData.observeEvent {
            FeeAssetSelectorBottomSheet(
                context = requireContext(),
                payload = it.payload,
                onOptionClicked = it.onSuccess,
                onCancel = it.onCancel
            ).show()
        }
    }
}
