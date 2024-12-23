package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationAccount
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationAlert
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationAssets
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationButton
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationExecutionTime
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationNetworkFee
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationPriceDifference
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationRate
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationRoute
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationSlippage
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationToolbar
import kotlinx.android.synthetic.main.fragment_swap_confirmation.swapConfirmationWallet

class SwapConfirmationFragment : BaseFragment<SwapConfirmationViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_confirmation, container, false)
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
        swapConfirmationRoute.setOnClickListener { viewModel.routeClicked() }
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

        viewModel.feeMixin.setupFeeLoading(swapConfirmationNetworkFee)

        viewModel.swapDetails.observe {
            swapConfirmationAssets.setModel(it.assets)
            swapConfirmationRate.showValue(it.rate)
            swapConfirmationPriceDifference.showValueOrHide(it.priceDifference)
            swapConfirmationSlippage.showValue(it.slippage)
            swapConfirmationRoute.setSwapRouteModel(it.swapRouteModel)
            swapConfirmationExecutionTime.showValue(it.estimatedExecutionTime)
        }

        viewModel.wallet.observe { swapConfirmationWallet.showWallet(it) }
        viewModel.addressFlow.observe { swapConfirmationAccount.showAddress(it) }

        viewModel.slippageAlertMixin.slippageAlertMessage.observe { swapConfirmationAlert.setMessageOrHide(it) }

        viewModel.validationInProgress.observe(swapConfirmationButton::setProgressState)
    }
}
