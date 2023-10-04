package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationAccount
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationAssetFrom
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationAssetTo
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationButton
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationNetworkFee
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationPriceDifference
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationRate
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationSlippage
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationToolbar
import kotlinx.android.synthetic.main.fragment_swap_confirmation_settings.swapConfirmationWallet

class SwapConfirmationFragment : BaseFragment<SwapConfirmationViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_confirmation_settings, container, false)
    }

    override fun initViews() {
        swapConfirmationToolbar.applyStatusBarInsets()
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
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapConfirmationViewModel) {
        viewModel.fromAsset.observe { swapConfirmationAssetFrom.setModel(it) }
        viewModel.toAsset.observe { swapConfirmationAssetTo.setModel(it) }
        viewModel.rateDetails.observe { swapConfirmationRate.showValue(it) }
        viewModel.priceDifference.observe { swapConfirmationPriceDifference.showValue(it) }
        viewModel.slippage.observe { swapConfirmationSlippage.showValue(it) }
        viewModel.networkFee.observe { swapConfirmationNetworkFee.showAmount(it) }
        viewModel.wallet.observe { swapConfirmationWallet.showWallet(it) }
        viewModel.account.observe { swapConfirmationAccount.showAddress(it) }
    }
}
