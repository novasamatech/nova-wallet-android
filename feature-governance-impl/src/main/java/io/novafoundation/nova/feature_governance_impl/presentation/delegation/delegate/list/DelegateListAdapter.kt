package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import coil.ImageLoader
import coil.clear
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.DEFAULT_CORNER_RADIUS
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.model.DelegateListModel
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateDelegatedVotes
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateDelegations
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateDescription
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateIcon
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateRecentVotes
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateRecentVotesLabel
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateTitle
import kotlinx.android.synthetic.main.item_delegate.view.itemDelegateType

class DelegateListAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : BaseListAdapter<DelegateListModel, DelegateViewHolder>(DelegateDiffCallback()) {

    interface Handler {

        fun itemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegateViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegate)

        return DelegateViewHolder(containerView, imageLoader, handler)
    }

    override fun onBindViewHolder(holder: DelegateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class DelegateDiffCallback : ItemCallback<DelegateListModel>() {

    override fun areItemsTheSame(oldItem: DelegateListModel, newItem: DelegateListModel): Boolean {
        return oldItem.accountId.contentEquals(newItem.accountId)
    }

    override fun areContentsTheSame(oldItem: DelegateListModel, newItem: DelegateListModel): Boolean {
        return oldItem.stats == newItem.stats
    }
}

class DelegateViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
    handler: DelegateListAdapter.Handler
) : BaseViewHolder(containerView) {

    init {
        with(containerView) {
            setOnClickListener { handler.itemClicked(bindingAdapterPosition) }

            background = with(context) { addRipple(getBlockDrawable()) }
        }
    }

    fun bind(model: DelegateListModel) = with(containerView) {
        itemDelegateIcon.setDelegateIcon(model.icon,imageLoader)
        itemDelegateTitle.text = model.name
        itemDelegateDescription.setTextOrHide(model.description)
        itemDelegateDelegations.text = model.stats.delegations
        itemDelegateDelegatedVotes.text = model.stats.delegatedVotes
        itemDelegateRecentVotes.text = model.stats.recentVotes.value
        itemDelegateRecentVotesLabel.text = model.stats.recentVotes.label

        itemDelegateType.setDelegateTypeModel(model.type)
    }

    override fun unbind() = with(containerView) {
        itemDelegateIcon.clear()
    }
}
