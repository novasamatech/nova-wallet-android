package io.novafoundation.nova.feature_assets.presentation.transaction.detail.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatDateTime
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailAmount
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailFee
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailFrom
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailHash
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailNetwork
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailRepeat
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailStatus
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailTo
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailToolbar
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailTransferDirection
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailTxSection

private const val KEY_TRANSACTION = "KEY_DRAFT"

class TransferDetailFragment : BaseFragment<TransactionDetailViewModel>() {

    companion object {
        fun getBundle(operation: OperationParcelizeModel.Transfer) = Bundle().apply {
            putParcelable(KEY_TRANSACTION, operation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_transfer_details, container, false)

    override fun initViews() {
        transactionDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        transactionDetailHash.setOnClickListener {
            viewModel.transactionHashClicked()
        }

        transactionDetailFrom.setOnClickListener {
            viewModel.fromAddressClicked()
        }

        transactionDetailTo.setOnClickListener {
            viewModel.toAddressClicked()
        }

        transactionDetailRepeat.setOnClickListener {
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
            transactionDetailStatus.setText(statusAppearance.labelRes)
            transactionDetailStatus.setDrawableStart(
                drawableRes = statusAppearance.icon,
                widthInDp = 16,
                paddingInDp = 4,
                tint = statusAppearance.statusTint
            )
            transactionDetailStatus.setTextColorRes(statusAppearance.statusTint)

            transactionDetailToolbar.setTitle(time.formatDateTime(requireContext()))

            transactionDetailFee.showValue(fee)

            transactionDetailAmount.text = amount
            transactionDetailAmount.setTextColorRes(statusAppearance.amountTint)

            if (hash != null) {
                transactionDetailHash.showValue(hash)
            } else {
                transactionDetailTxSection.makeGone()
            }
        }

        viewModel.senderAddressModelLiveData.observe(transactionDetailFrom::showAddress)
        viewModel.recipientAddressModelFlow.observe(transactionDetailTo::showAddress)

        viewModel.chainUi.observe(transactionDetailNetwork::showChain)

        transactionDetailTransferDirection.setImageResource(viewModel.transferDirection)
    }
}
