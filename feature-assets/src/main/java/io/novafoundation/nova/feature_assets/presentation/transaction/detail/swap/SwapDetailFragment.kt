package io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_assets.databinding.FragmentSwapDetailsBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_wallet_api.presentation.view.showLoadingAmount

private const val KEY_PAYLOAD = "SwapDetailFragment.Payload"

class SwapDetailFragment : BaseFragment<SwapDetailViewModel, FragmentSwapDetailsBinding>() {

    companion object {
        fun getBundle(operation: OperationParcelizeModel.Swap) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, operation)
        }
    }

    override fun createBinding() = FragmentSwapDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.swapDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.swapDetailHash.setOnClickListener {
            viewModel.transactionHashClicked()
        }

        binder.swapDetailAccount.setOnClickListener {
            viewModel.originAddressClicked()
        }

        binder.swapDetailRate.setOnClickListener {
            viewModel.rateClicked()
        }

        binder.swapDetailFee.setOnClickListener {
            viewModel.feeClicked()
        }

        binder.swapDetailsRepeatOperation.setOnClickListener {
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
            binder.swapDetailStatus.showOperationStatus(statusAppearance)
            binder.swapDetailToolbar.setTitle(timeMillis.formatDateTime())

            binder.swapDetailAmount.setTokenAmountTextColor(statusAppearance.amountTint)

            binder.swapDetailHash.showValueOrHide(transactionHash)
        }

        viewModel.amountModel.observe(binder.swapDetailAmount::setAmount)

        viewModel.assetInModel.observe(binder.swapDetailAssets::setAssetIn)
        viewModel.assetOutModel.observe(binder.swapDetailAssets::setAssetOut)

        viewModel.rate.observe(binder.swapDetailRate::showValue)
        viewModel.feeModel.observe(binder.swapDetailFee::showLoadingAmount)

        viewModel.walletUi.observe(binder.swapDetailWallet::showWallet)
        viewModel.originAddressModelFlow.observe(binder.swapDetailAccount::showAddress)
    }
}
