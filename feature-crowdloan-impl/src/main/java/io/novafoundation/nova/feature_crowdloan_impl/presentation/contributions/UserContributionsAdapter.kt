package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model.ContributionModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.setIcon
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_contribution.view.itemContributionAmount
import kotlinx.android.synthetic.main.item_contribution.view.itemContributionIcon
import kotlinx.android.synthetic.main.item_contribution.view.itemContributionName

class UserContributionsAdapter(
    private val imageLoader: ImageLoader,
) : ListAdapter<ContributionModel, ContributionHolder>(ContributionCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributionHolder {
        return ContributionHolder(imageLoader, parent.inflateChild(R.layout.item_contribution))
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
        return oldItem.name == newItem.name
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
    override val containerView: View,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
        item: ContributionModel,
    ) = with(containerView) {
        itemContributionIcon.setIcon(item.icon, imageLoader)
        itemContributionName.text = item.name

        bindAmount(item)
    }

    fun unbind() {
        with(containerView) {
            itemContributionIcon.clear()
        }
    }

    fun bindAmount(item: ContributionModel) {
        containerView.itemContributionAmount.text = item.amount
    }
}
