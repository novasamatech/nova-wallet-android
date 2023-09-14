package io.novafoundation.nova.feature_assets.presentation.transaction.history

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.formatting.formatDaysSinceEpoch
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.recyclerview.item.OperationListItem
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.model.OperationModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_assets.presentation.transaction.history.model.DayHeader
import kotlinx.android.synthetic.main.item_day_header.view.itemDayHeader

class TransactionHistoryAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader,
) : GroupedListAdapter<DayHeader, OperationModel>(TransactionHistoryDiffCallback) {

    interface Handler {

        fun transactionClicked(transactionId: String)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return DayHolder(parent.inflateChild(R.layout.item_day_header))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return TransactionHolder(OperationListItem(parent.context), imageLoader)
    }

    override fun bindGroup(holder: GroupedListHolder, group: DayHeader) {
        (holder as DayHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: OperationModel) {
        (holder as TransactionHolder).bind(child, handler)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: OperationModel, payloads: List<Any>) {
        require(holder is TransactionHolder)

        resolvePayload(holder, position, payloads) {
            when (it) {
                OperationModel::statusAppearance -> holder.bindStatus(child)
                OperationModel::header -> holder.bindHeader(child)
                OperationModel::subHeader -> holder.bindSubHeader(child)
                OperationModel::amount -> holder.bindAmount(child)
                OperationModel::amountDetails -> holder.bindAmountDetails(child)
            }
        }
    }
}

class TransactionHolder(
    override val containerView: OperationListItem,
    private val imageLoader: ImageLoader
) : GroupedListHolder(containerView) {

    init {
        containerView.setIconStyle(OperationListItem.IconStyle.BORDERED_CIRCLE)
    }

    fun bind(item: OperationModel, handler: TransactionHistoryAdapter.Handler) {
        with(containerView) {
            bindHeader(item)

            bindAmount(item)

            bindAmountDetails(item)
            bindSubHeader(item)

            icon.setIcon(item.operationIcon, imageLoader)

            bindStatus(item)

            setOnClickListener { handler.transactionClicked(item.id) }
        }
    }

    fun bindAmount(item: OperationModel) = with(containerView) {
        valuePrimary.setTextColorRes(item.amountColorRes)
        valuePrimary.text = item.amount
    }

    fun bindHeader(item: OperationModel) {
        containerView.header.text = item.header
    }

    fun bindAmountDetails(item: OperationModel) {
        containerView.valueSecondary.setTextOrHide(item.amountDetails)
    }

    fun bindSubHeader(item: OperationModel) = with(containerView) {
        subHeader.text = item.subHeader
        subHeader.ellipsize = item.subHeaderEllipsize
    }

    fun bindStatus(item: OperationModel) = with(containerView) {
        if (item.statusAppearance != OperationStatusAppearance.COMPLETED) {
            status.makeVisible()
            status.setImageResource(item.statusAppearance.icon)
            status.setImageTintRes(item.statusAppearance.statusIconTint)
        } else {
            status.makeGone()
        }
    }

    override fun unbind() {
        containerView.icon.clear()
    }
}

class DayHolder(view: View) : GroupedListHolder(view) {
    fun bind(item: DayHeader) {
        with(containerView) {
            itemDayHeader.text = item.daysSinceEpoch.formatDaysSinceEpoch(context)
        }
    }
}

private object TransactionPayloadGenerator : PayloadGenerator<OperationModel>(
    OperationModel::statusAppearance,
    OperationModel::header,
    OperationModel::subHeader,
    OperationModel::amount,
    OperationModel::amountDetails,
)

object TransactionHistoryDiffCallback : BaseGroupedDiffCallback<DayHeader, OperationModel>(DayHeader::class.java) {
    override fun areGroupItemsTheSame(oldItem: DayHeader, newItem: DayHeader): Boolean {
        return oldItem.daysSinceEpoch == newItem.daysSinceEpoch
    }

    override fun areGroupContentsTheSame(oldItem: DayHeader, newItem: DayHeader): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: OperationModel, newItem: OperationModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: OperationModel, newItem: OperationModel): Boolean {
        return oldItem.statusAppearance == newItem.statusAppearance &&
            oldItem.header == newItem.header &&
            oldItem.subHeader == newItem.subHeader &&
            oldItem.amount == newItem.amount &&
            oldItem.amountDetails == newItem.amountDetails
    }

    override fun getChildChangePayload(oldItem: OperationModel, newItem: OperationModel): Any? {
        return TransactionPayloadGenerator.diff(oldItem, newItem)
    }
}
