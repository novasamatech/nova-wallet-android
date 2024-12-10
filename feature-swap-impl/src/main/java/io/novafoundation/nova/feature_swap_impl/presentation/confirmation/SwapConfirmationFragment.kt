package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapConfirmationBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading

class SwapConfirmationFragment : BaseFragment<SwapConfirmationViewModel, FragmentSwapConfirmationBinding>() {

    override fun createBinding() = FragmentSwapConfirmationBinding.inflate(layoutInflater)

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
        binder.swapConfirmationRoute.setOnClickListener { viewModel.routeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapConfirmation()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapConfirmationViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeDescription(viewModel)

        viewModel.feeMixin.setupFeeLoading(binder.swapConfirmationNetworkFee)

        viewModel.swapDetails.observe {
            binder.swapConfirmationAssets.setModel(it.assets)
            binder.swapConfirmationRate.showValue(it.rate)
            binder.swapConfirmationPriceDifference.showValueOrHide(it.priceDifference)
            binder.swapConfirmationSlippage.showValue(it.slippage)
            binder.swapConfirmationRoute.setSwapRouteModel(it.swapRouteModel)
            binder.swapConfirmationExecutionTime.showValue(it.estimatedExecutionTime)
        }

        viewModel.wallet.observe { binder.swapConfirmationWallet.showWallet(it) }
        viewModel.addressFlow.observe { binder.swapConfirmationAccount.showAddress(it) }

        viewModel.slippageAlertMixin.slippageAlertMessage.observe { binder.swapConfirmationAlert.setMessageOrHide(it) }

        viewModel.validationInProgress.observe(binder.swapConfirmationButton::setProgressState)
    }
}
