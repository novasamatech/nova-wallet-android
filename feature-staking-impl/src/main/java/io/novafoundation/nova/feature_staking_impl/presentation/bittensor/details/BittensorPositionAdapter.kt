package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorPositionBinding

class BittensorPositionAdapter(
    private val imageLoader: ImageLoader,
    private val onClick: (BittensorPositionUi) -> Unit
) : ListAdapter<BittensorPositionUi, BittensorPositionAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemBittensorPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemBittensorPositionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BittensorPositionUi) = with(binding) {
            positionIcon.updateLayoutParams {
                width = item.iconSizeDp.dp(root.context)
                height = item.iconSizeDp.dp(root.context)
            }
            positionIcon.setIcon(item.icon, imageLoader) {
                transformations(CircleCropTransformation())
                error(R.drawable.ic_fallback_network_icon)
            }
            positionTitle.text = item.title
            val isValidator = item.action is BittensorPositionAction.ValidatorDetails
            positionTitle.setTextAppearance(
                if (isValidator) R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline else R.style.TextAppearance_NovaFoundation_SemiBold_Body
            )
            positionAmount.setTextAppearance(
                if (isValidator) R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline else R.style.TextAppearance_NovaFoundation_SemiBold_Body
            )
            positionSubtitle.setTextAppearance(
                if (isValidator) {
                    R.style.TextAppearance_NovaFoundation_Regular_Caption1_Secondary
                } else {
                    R.style.TextAppearance_NovaFoundation_Regular_Footnote_Secondary
                }
            )
            positionFiat.setTextAppearance(
                if (isValidator) {
                    R.style.TextAppearance_NovaFoundation_Regular_Caption1_Secondary
                } else {
                    R.style.TextAppearance_NovaFoundation_Regular_Footnote_Secondary
                }
            )
            positionSubtitle.text = item.subtitle
            positionSubtitle.isVisible = !item.isSubtitleLoading && item.subtitle != null
            positionSubtitleProgress.isVisible = item.isSubtitleLoading
            positionAmount.text = item.amount.token
            positionAmount.setTextColorRes(item.amountColorRes)
            positionFiat.text = item.amount.fiat
            positionAmount.isVisible = !item.isAmountLoading && item.amount.token.isNotEmpty()
            positionAmountProgress.isVisible = item.isAmountLoading
            positionFiat.isVisible = !item.isAmountLoading && item.amount.token.isNotEmpty() && item.amount.fiat != null
            positionChange.text = item.change?.text
            positionChange.isVisible = !item.isChangeLoading && item.change != null
            positionChange.setTextColorRes(
                when {
                    item.change?.isFlat == true -> R.color.text_secondary
                    item.change?.isRising == false -> R.color.text_negative
                    else -> R.color.text_positive
                }
            )
            positionChangeProgress.isVisible = item.isChangeLoading
            positionChevron.isVisible = true
            root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<BittensorPositionUi>() {

        override fun areItemsTheSame(oldItem: BittensorPositionUi, newItem: BittensorPositionUi) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BittensorPositionUi, newItem: BittensorPositionUi) = oldItem == newItem
    }
}
