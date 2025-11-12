package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.recyclerview.item.OperationListItem
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model.ContributionModel
import kotlinx.android.extensions.LayoutContainer

class UserContributionsAdapter(
    private val imageLoader: ImageLoader,
) : ListAdapter<ContributionModel, ContributionHolder>(ContributionCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributionHolder {
        return ContributionHolder(imageLoader, OperationListItem(parent.context))
    }

    override fun onBindViewHolder(holder: ContributionHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ContributionHolder, position: Int, payloads: MutableList<Any>) {
        resolvePayload(holder, position, payloads) {
            when (it) {
                ContributionModel::amount -> holder.bindAmount(getItem(position))
            }
        }
    }

    override fun onViewRecycled(holder: ContributionHolder) {
        holder.unbind()
    }
}

private object ContributionPayloadGenerator : PayloadGenerator<ContributionModel>(
    ContributionModel::amount
)

private object ContributionCallback : DiffUtil.ItemCallback<ContributionModel>() {

    override fun areItemsTheSame(oldItem: ContributionModel, newItem: ContributionModel): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: ContributionModel, newItem: ContributionModel): Boolean {
        return true
    }

    override fun getChangePayload(oldItem: ContributionModel, newItem: ContributionModel): Any? {
        return ContributionPayloadGenerator.diff(oldItem, newItem)
    }
}

class ContributionHolder(
    private val imageLoader: ImageLoader,
    override val containerView: OperationListItem,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setIconStyle(OperationListItem.IconStyle.DEFAULT)
    }

    fun bind(
        item: ContributionModel,
    ) = with(containerView) {
        icon.setIcon(item.icon, imageLoader)

        header.text = item.title

        subHeader.setTextColorRes(item.claimStatusColorRes)
        when(val status = item.claimStatus) {
            is ContributionModel.ClaimStatus.Text -> {
                subHeader.stopTimer()
                subHeader.text = status.text
            }

            is ContributionModel.ClaimStatus.Timer -> subHeader.startTimer(
                value = status.timer,
                customMessageFormat = R.string.crowdloan_contributions_returns_in,
                onFinish = { subHeader.setText(R.string.crowdloan_contribution_claimable) }
            )
        }

        bindAmount(item)
    }

    fun unbind() {
        containerView.icon.clear()
    }

    fun bindAmount(item: ContributionModel) {
        containerView.valuePrimary.text = item.amount.token
        containerView.valueSecondary.setTextOrHide(item.amount.fiat)
    }
}
