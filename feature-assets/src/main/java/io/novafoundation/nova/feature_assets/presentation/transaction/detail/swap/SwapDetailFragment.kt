package io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_wallet_api.presentation.view.showLoadingAmount
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailAccount
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailAmount
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailAssets
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailFee
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailHash
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailRate
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailStatus
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailToolbar
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailWallet
import kotlinx.android.synthetic.main.fragment_swap_details.swapDetailsRepeatOperation

private const val KEY_PAYLOAD = "SwapDetailFragment.Payload"

class SwapDetailFragment : BaseFragment<SwapDetailViewModel>() {

    companion object {
        fun getBundle(operation: OperationParcelizeModel.Swap) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, operation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_swap_details, container, false)

    override fun initViews() {
        swapDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        swapDetailHash.setOnClickListener {
            viewModel.transactionHashClicked()
        }

        swapDetailAccount.setOnClickListener {
            viewModel.originAddressClicked()
        }

        swapDetailRate.setOnClickListener {
            viewModel.rateClicked()
        }

        swapDetailFee.setOnClickListener {
            viewModel.feeClicked()
        }

        swapDetailsRepeatOperation.setOnClickListener {
            viewModel.repeatOperationClicked()
        }
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.Swap>(KEY_PAYLOAD)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .swapDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapDetailViewModel) {
        setupExternalActions(viewModel)
        observeDescription(viewModel)

        with(viewModel.operation) {
            swapDetailStatus.showOperationStatus(statusAppearance)
            swapDetailToolbar.setTitle(timeMillis.formatDateTime())

            swapDetailAmount.setTokenAmountTextColor(statusAppearance.amountTint)

            swapDetailHash.showValueOrHide(transactionHash)
        }

        viewModel.amountModel.observe(swapDetailAmount::setAmount)

        viewModel.assetInModel.observe(swapDetailAssets::setAssetIn)
        viewModel.assetOutModel.observe(swapDetailAssets::setAssetOut)

        viewModel.rate.observe(swapDetailRate::showValue)
        viewModel.feeModel.observe(swapDetailFee::showLoadingAmount)

        viewModel.walletUi.observe(swapDetailWallet::showWallet)
        viewModel.originAddressModelFlow.observe(swapDetailAccount::showAddress)
    }
}
