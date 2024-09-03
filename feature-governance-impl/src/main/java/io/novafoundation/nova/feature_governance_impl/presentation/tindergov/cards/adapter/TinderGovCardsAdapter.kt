package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoadingOrError
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardsAdapter.Handler
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardAmountContainer
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardAmountShimmering
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardDivider
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardRequestedAmount
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardRequestedFiat
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardSummary
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.itemTinderGovCardSummaryShimmering
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.tinderGovCardContainer
import kotlinx.android.synthetic.main.item_tinder_gov_card.view.tinderGovCardReadMore

class TinderGovCardsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val handler: Handler
) : ListAdapter<TinderGovCardRvItem, TinderGovCardViewHolder>(TinderGovCardsDiffCallback()) {

    interface Handler {
        fun onReadMoreClicked(item: TinderGovCardRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TinderGovCardViewHolder {
        return TinderGovCardViewHolder(handler, lifecycleOwner, parent.inflateChild(R.layout.item_tinder_gov_card))
    }

    override fun onBindViewHolder(holder: TinderGovCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TinderGovCardViewHolder(
    private val handler: Handler,
    lifecycleOwner: LifecycleOwner,
    containerView: View
) : GroupedListHolder(containerView) {

    init {
        itemView.tinderGovCardReadMore.prepareForProgress(lifecycleOwner)
    }

    fun bind(item: TinderGovCardRvItem) {
        itemView.itemTinderGovCardSummary.setTextOrHide(item.summary.dataOrNull)
        itemView.itemTinderGovCardSummaryShimmering.isVisible = item.summary.isLoadingOrError()

        itemView.itemTinderGovCardAmountContainer.letOrHide(item.requestedAmount.dataOrNull) {
            itemView.itemTinderGovCardRequestedAmount.setTextOrHide(it.token)
            itemView.itemTinderGovCardRequestedFiat.setTextOrHide(it.fiat)
        }

        itemView.itemTinderGovCardDivider.isVisible = item.requestedAmount.isLoadingOrError() || item.requestedAmount.dataOrNull != null
        itemView.itemTinderGovCardAmountShimmering.isVisible = item.requestedAmount.isLoadingOrError()
        itemView.tinderGovCardContainer.setBackgroundResource(item.backgroundRes)

        itemView.tinderGovCardReadMore.setOnClickListener { handler.onReadMoreClicked(item) }
    }
}

private class TinderGovCardsDiffCallback : DiffUtil.ItemCallback<TinderGovCardRvItem>() {

    override fun areItemsTheSame(oldItem: TinderGovCardRvItem, newItem: TinderGovCardRvItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TinderGovCardRvItem, newItem: TinderGovCardRvItem): Boolean {
        return oldItem == newItem
    }
}
