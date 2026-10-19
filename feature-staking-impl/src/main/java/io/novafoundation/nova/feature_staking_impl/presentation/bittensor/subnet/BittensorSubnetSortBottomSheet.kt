package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.input.chooser.ListChooserBottomSheet
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin.Model
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorSubnetSortBinding
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSorting

class BittensorSubnetSortBottomSheet(
    context: Context,
    private val payload: ListChooserBottomSheet.Payload<Model<BittensorSubnetSorting>>,
    onClicked: ClickHandler<Model<BittensorSubnetSorting>>,
    onCancel: () -> Unit
) : DynamicListBottomSheet<Model<BittensorSubnetSorting>>(context, payload, Diff, onClicked, onCancel) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(payload.titleRes)
        recyclerView.background = context.getRoundedCornerDrawable(R.color.block_background, cornerSizeInDp = 12)
        recyclerView.clipToOutline = true
        recyclerView.updatePadding(bottom = 0)
        recyclerView.updateLayoutParams<MarginLayoutParams> {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            bottomMargin = 24.dp(context)
        }
    }

    override fun holderCreator(): HolderCreator<Model<BittensorSubnetSorting>> = { parent ->
        Holder(ItemBittensorSubnetSortBinding.inflate(LayoutInflater.from(parent.context), parent, false), payload.data.last().value)
    }

    private class Holder(
        private val binder: ItemBittensorSubnetSortBinding,
        private val lastOption: BittensorSubnetSorting
    ) : DynamicListSheetAdapter.Holder<Model<BittensorSubnetSorting>>(binder.root) {

        override fun bind(item: Model<BittensorSubnetSorting>, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<Model<BittensorSubnetSorting>>) {
            super.bind(item, isSelected, handler)
            binder.title.text = item.display
            binder.radio.isChecked = isSelected
            binder.description.setText(item.value.descriptionRes())
            binder.divider.isVisible = item.value != lastOption
        }
    }

    private object Diff : DiffUtil.ItemCallback<Model<BittensorSubnetSorting>>() {

        override fun areItemsTheSame(oldItem: Model<BittensorSubnetSorting>, newItem: Model<BittensorSubnetSorting>) = oldItem.value == newItem.value

        override fun areContentsTheSame(oldItem: Model<BittensorSubnetSorting>, newItem: Model<BittensorSubnetSorting>) = oldItem.display == newItem.display
    }
}

private fun BittensorSubnetSorting.descriptionRes(): Int = when (this) {
    BittensorSubnetSorting.POOL_DEPTH -> R.string.bittensor_sort_pool_description
    BittensorSubnetSorting.AGE -> R.string.bittensor_sort_age_description
    BittensorSubnetSorting.APY -> R.string.bittensor_sort_apy_description
    BittensorSubnetSorting.SEVEN_DAY_CHANGE -> R.string.bittensor_sort_week_description
    BittensorSubnetSorting.NAME -> R.string.bittensor_sort_name_description
}
