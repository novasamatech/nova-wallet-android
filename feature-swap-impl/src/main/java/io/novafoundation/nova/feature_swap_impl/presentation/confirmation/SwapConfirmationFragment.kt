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
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapConfirmationSettingsBinding
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
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class SwapConfirmationFragment : BaseFragment<SwapConfirmationViewModel, FragmentSwapConfirmationSettingsBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_confirmation, container, false)
    }

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
