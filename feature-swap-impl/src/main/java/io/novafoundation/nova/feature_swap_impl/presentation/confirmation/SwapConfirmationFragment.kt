package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapConfirmationSettingsBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class SwapConfirmationFragment : BaseFragment<SwapConfirmationViewModel, FragmentSwapConfirmationSettingsBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "SwapConfirmationFragment.Payload"

        fun getBundle(payload: SwapConfirmationPayload) = bundleOf(KEY_PAYLOAD to payload)
    }

    override val binder by viewBinding(FragmentSwapConfirmationSettingsBinding::bind)

    override fun initViews() {
        binder.swapConfirmationToolbar.applyStatusBarInsets()
        binder.swapConfirmationToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.swapConfirmationButton.prepareForProgress(this)
        binder.swapConfirmationRate.setOnClickListener { viewModel.rateClicked() }
        binder.swapConfirmationPriceDifference.setOnClickListener { viewModel.priceDifferenceClicked() }
        binder.swapConfirmationSlippage.setOnClickListener { viewModel.slippageClicked() }
        binder.swapConfirmationNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        binder.swapConfirmationAccount.setOnClickListener { viewModel.accountClicked() }
        binder.swapConfirmationButton.setOnClickListener { viewModel.confirmButtonClicked() }
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
        setupFeeLoading(viewModel.feeMixin, binder.swapConfirmationNetworkFee)

        viewModel.swapDetails.observe {
            binder.swapConfirmationAssets.setModel(it.assets)
            binder.swapConfirmationRate.showValue(it.rate)
            binder.swapConfirmationPriceDifference.showValueOrHide(it.priceDifference)
            binder.swapConfirmationSlippage.showValue(it.slippage)
        }

        viewModel.wallet.observe { binder.swapConfirmationWallet.showWallet(it) }
        viewModel.addressFlow.observe { binder.swapConfirmationAccount.showAddress(it) }

        viewModel.slippageAlertMessage.observe { binder.swapConfirmationAlert.setMessageOrHide(it) }

        viewModel.validationProgress.observe(binder.swapConfirmationButton::setProgressState)
    }
}
