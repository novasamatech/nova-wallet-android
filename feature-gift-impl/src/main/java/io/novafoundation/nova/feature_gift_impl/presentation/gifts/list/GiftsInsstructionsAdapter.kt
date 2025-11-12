package io.novafoundation.nova.feature_gift_impl.presentation.gifts.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.spannableFormatting
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.databinding.ItemGiftsInstructionPlaceholderBinding

class GiftsInstructionsAdapter() : SingleItemAdapter<GiftsInstructionsHolder>(isShownByDefault = false) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiftsInstructionsHolder {
        return GiftsInstructionsHolder(ItemGiftsInstructionPlaceholderBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: GiftsInstructionsHolder, position: Int) {
    }
}

class GiftsInstructionsHolder(binder: ItemGiftsInstructionPlaceholderBinding) : RecyclerView.ViewHolder(binder.root) {

    init {
        with(binder.root.context) {
            val highlightColor = getColor(R.color.text_primary)
            binder.giftsInstructionStep1.setStepText(getString(R.string.gifts_placeholder_step_1).addColor(highlightColor))
            binder.giftsInstructionStep2.setStepText(
                getString(R.string.gifts_placeholder_step_2)
                    .spannableFormatting(getString(R.string.gifts_placeholder_step_2_highlight).addColor(highlightColor))
            )
            binder.giftsInstructionStep3.setStepText(
                getString(R.string.gifts_placeholder_step_3)
                    .spannableFormatting(getString(R.string.gifts_placeholder_step_3_highlight).addColor(highlightColor))
            )
        }
    }
}
