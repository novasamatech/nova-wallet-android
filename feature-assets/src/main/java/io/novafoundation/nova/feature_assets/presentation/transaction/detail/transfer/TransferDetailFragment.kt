package io.novafoundation.nova.feature_assets.presentation.transaction.detail.transfer

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_account_api.view.showOptionalAddress
import io.novafoundation.nova.feature_assets.databinding.FragmentTransferDetailsBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_assets.presentation.model.toAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showLoadingAmount

private const val KEY_TRANSACTION = "KEY_DRAFT"

class TransferDetailFragment : BaseFragment<TransactionDetailViewModel, FragmentTransferDetailsBinding>() {

    companion object {
        fun getBundle(operation: OperationParcelizeModel.Transfer) = Bundle().apply {
            putParcelable(KEY_TRANSACTION, operation)
        }
    }

    override fun createBinding() = FragmentTransferDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.transactionDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.transactionDetailHash.setOnClickListener {
            viewModel.transactionHashClicked()
        }

        binder.transactionDetailFrom.setOnClickListener {
            viewModel.fromAddressClicked()
        }

        binder.transactionDetailTo.setOnClickListener {
            viewModel.toAddressClicked()
        }

        binder.transactionDetailRepeat.setOnClickListener {
            viewModel.repeatTransaction()
        }
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.Transfer>(KEY_TRANSACTION)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .transactionDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    override fun subscribe(viewModel: TransactionDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            binder.transactionDetailStatus.showOperationStatus(statusAppearance)
            binder.transactionDetailTransferDirection.setImageResource(transferDirectionIcon)

            binder.transactionDetailToolbar.setTitle(time.formatDateTime())

            viewModel.fee.observe(binder.transactionDetailFee::showLoadingAmount)

            binder.transactionDetailAmount.setAmount(amount.toAmountModel())
            binder.transactionDetailAmount.setTokenAmountTextColor(statusAppearance.amountTint)

            binder.transactionDetailHash.showValueOrHide(hash)
        }

        viewModel.senderAddressModelLiveData.observe(binder.transactionDetailFrom::showOptionalAddress)
        viewModel.recipientAddressModelFlow.observe(binder.transactionDetailTo::showOptionalAddress)

        viewModel.chainUi.observe(binder.transactionDetailNetwork::showChain)
    }
}
