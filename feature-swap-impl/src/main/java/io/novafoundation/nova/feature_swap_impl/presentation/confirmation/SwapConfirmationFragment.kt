package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class SwapConfirmationFragment : BaseFragment<SwapConfirmationViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "SwapConfirmationFragment.Payload"

        fun getBundle(payload: SwapConfirmationPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_confirmation_settings, container, false)
    }

    override fun initViews() {
        swapConfirmationToolbar.applyStatusBarInsets()
        swapConfirmationToolbar.setHomeButtonListener { viewModel.backClicked() }
        swapConfirmationButton.prepareForProgress(this)
        swapConfirmationRate.setOnClickListener { viewModel.rateClicked() }
        swapConfirmationPriceDifference.setOnClickListener { viewModel.priceDifferenceClicked() }
        swapConfirmationSlippage.setOnClickListener { viewModel.slippageClicked() }
        swapConfirmationNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        swapConfirmationAccount.setOnClickListener { viewModel.accountClicked() }
        swapConfirmationButton.setOnClickListener { viewModel.confirmButtonClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapConfirmation()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SwapConfirmationViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeDescription(viewModel)
        setupFeeLoading(viewModel.feeMixin, swapConfirmationNetworkFee)

        viewModel.swapDetails.observe {
            swapConfirmationAssets.setModel(it.assets)
            swapConfirmationRate.showValue(it.rate)
            swapConfirmationPriceDifference.showValueOrHide(it.priceDifference)
            swapConfirmationSlippage.showValue(it.slippage)
        }

        viewModel.wallet.observe { swapConfirmationWallet.showWallet(it) }
        viewModel.addressFlow.observe { swapConfirmationAccount.showAddress(it) }

        viewModel.slippageAlertMessage.observe { swapConfirmationAlert.setMessageOrHide(it) }

        viewModel.validationProgress.observe(swapConfirmationButton::setProgressState)
    }
}
