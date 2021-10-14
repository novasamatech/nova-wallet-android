package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.di.FeatureUtils
import jp.co.soramitsu.common.utils.formatDateTime
import jp.co.soramitsu.common.utils.makeGone
import jp.co.soramitsu.common.utils.makeInvisible
import jp.co.soramitsu.common.utils.makeVisible
import jp.co.soramitsu.common.utils.setTextColorRes
import jp.co.soramitsu.feature_account_api.presenatation.actions.setupExternalActions
import jp.co.soramitsu.feature_wallet_api.di.WalletFeatureApi
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.di.WalletFeatureComponent
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationStatusAppearance
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailAmount
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailDate
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailDivider4
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailDivider5
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailFee
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailFeeLabel
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailFrom
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailHash
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailRepeat
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailStatus
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailStatusIcon
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailTo
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailToolbar
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailTotal
import kotlinx.android.synthetic.main.fragment_transfer_details.transactionDetailTotalLabel

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

        transactionDetailHash.setWholeClickListener {
            viewModel.transactionHashClicked()
        }

        transactionDetailFrom.setWholeClickListener {
            viewModel.fromAddressClicked()
        }

        transactionDetailTo.setWholeClickListener {
            viewModel.toAddressClicked()
        }

        transactionDetailRepeat.setWholeClickListener {
            viewModel.repeatTransaction()
        }
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.Transfer>(KEY_TRANSACTION)

        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
        )
            .transactionDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    private fun amountColorRes(operation: OperationParcelizeModel.Transfer) = when {
        operation.statusAppearance == OperationStatusAppearance.FAILED -> R.color.gray2
        operation.isIncome -> R.color.green
        else -> R.color.white
    }

    override fun subscribe(viewModel: TransactionDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            transactionDetailStatus.setText(statusAppearance.labelRes)
            transactionDetailStatusIcon.setImageResource(statusAppearance.icon)

            transactionDetailDate.text = time.formatDateTime(requireContext())

            if (isIncome) {
                hideOutgoingViews()
            } else {
                showOutgoungViews()
                transactionDetailFee.text = fee
                transactionDetailTotal.text = total
            }

            transactionDetailAmount.text = amount
            transactionDetailAmount.setTextColorRes(amountColorRes(this))

            if (hash != null) {
                transactionDetailHash.setMessage(hash)
            } else {
                transactionDetailHash.makeGone()
            }
        }

        viewModel.senderAddressModelLiveData.observe { addressModel ->
            transactionDetailFrom.setMessage(addressModel.nameOrAddress)
            transactionDetailFrom.setTextIcon(addressModel.image)
        }

        viewModel.recipientAddressModelLiveData.observe { addressModel ->
            transactionDetailTo.setMessage(addressModel.nameOrAddress)
            transactionDetailTo.setTextIcon(addressModel.image)
        }

        viewModel.retryAddressModelLiveData.observe { addressModel ->
            val name = addressModel.name
            if (name != null) {
                transactionDetailRepeat.setTitle(name)
                transactionDetailRepeat.setText(addressModel.address)
                transactionDetailRepeat.showBody()
            } else {
                transactionDetailRepeat.setTitle(addressModel.address)
                transactionDetailRepeat.hideBody()
            }
            transactionDetailRepeat.setAccountIcon(addressModel.image)
        }
    }

    private fun hideOutgoingViews() {
        transactionDetailFee.makeGone()
        transactionDetailTotalLabel.makeGone()
        transactionDetailFeeLabel.makeGone()
        transactionDetailTotal.makeGone()
        transactionDetailDivider4.makeInvisible()
        transactionDetailDivider5.makeInvisible()
    }

    private fun showOutgoungViews() {
        transactionDetailFee.makeVisible()
        transactionDetailTotalLabel.makeVisible()
        transactionDetailFeeLabel.makeVisible()
        transactionDetailTotal.makeVisible()
        transactionDetailDivider4.makeVisible()
        transactionDetailDivider5.makeVisible()
    }
}
