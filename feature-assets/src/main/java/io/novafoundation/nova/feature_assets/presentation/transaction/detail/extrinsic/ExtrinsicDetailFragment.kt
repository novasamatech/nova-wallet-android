package io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.TableView
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.model.ExtrinsicContentModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.model.ExtrinsicContentModel.BlockEntry
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicContentContainer
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailAmount
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailIcon
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailNetwork
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailSender
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailStatus
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailToolbar
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailAmountFiat

private const val KEY_EXTRINSIC = "KEY_EXTRINSIC"

class ExtrinsicDetailFragment : BaseFragment<ExtrinsicDetailViewModel>() {

    companion object {
        fun getBundle(operation: OperationParcelizeModel.Extrinsic) = Bundle().apply {
            putParcelable(KEY_EXTRINSIC, operation)
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_extrinsic_details, container, false)

    override fun initViews() {
        extrinsicDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        extrinsicDetailSender.setOnClickListener {
            viewModel.fromAddressClicked()
        }
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.Extrinsic>(KEY_EXTRINSIC)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .extrinsicDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    override fun subscribe(viewModel: ExtrinsicDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            extrinsicDetailStatus.showOperationStatus(statusAppearance)
            extrinsicDetailAmount.setTextColorRes(statusAppearance.amountTint)

            extrinsicDetailToolbar.setTitle(time.formatDateTime())

            extrinsicDetailAmount.text = fee
            extrinsicDetailAmountFiat.setTextOrHide(this.fiatFee)
        }

        viewModel.content.observe(::showExtrinsicContent)

        viewModel.senderAddressModelFlow.observe(extrinsicDetailSender::showAddress)

        viewModel.chainUi.observe(extrinsicDetailNetwork::showChain)

        viewModel.operationIcon.observe {
            extrinsicDetailIcon.setIcon(it, imageLoader)
        }
    }

    private fun showExtrinsicContent(content: ExtrinsicContentModel) {
        content.blocks.forEach { block ->
            createBlock {
                block.entries.forEach { entry ->
                    blockEntry(entry)
                }
            }
        }
    }

    private fun createBlock(builder: TableView.() -> Unit) {
        val block = TableView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(16.dp, 12.dp, 16.dp, 0)
            }
        }

        block.apply(builder)

        extrinsicContentContainer.addView(block)
    }

    private fun TableView.blockEntry(entry: BlockEntry) {
        when (entry) {
            is BlockEntry.Address -> address(entry)
            is BlockEntry.LabeledValue -> labeledValue(entry)
            is BlockEntry.TransactionId -> transactionId(entry)
        }
    }

    private fun TableView.transactionId(transactionId: BlockEntry.TransactionId) {
        createEntry {
            setTitle(transactionId.label)
            showValue(transactionId.hash)

            clickable { viewModel.transactionIdClicked(transactionId.hash) }
        }
    }

    private fun TableView.address(address: BlockEntry.Address) {
        createEntry {
            setTitle(address.label)
            showAddress(address.addressModel)

            clickable { viewModel.addressClicked(address.addressModel.address) }
        }
    }

    private fun TableView.labeledValue(labeledValue: BlockEntry.LabeledValue) {
        createEntry {
            setTitle(labeledValue.label)
            showValue(labeledValue.value)
        }
    }

    private fun TableView.createEntry(builder: TableCellView.() -> Unit) {
        val block = TableCellView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        block.apply(builder)

        addView(block)
    }

    private inline fun TableCellView.clickable(crossinline onClick: () -> Unit) {
        setPrimaryValueEndIcon(R.drawable.ic_info)

        setOnClickListener { onClick() }
    }
}
