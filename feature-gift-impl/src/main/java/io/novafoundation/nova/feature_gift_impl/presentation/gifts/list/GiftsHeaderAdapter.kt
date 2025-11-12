package io.novafoundation.nova.feature_gift_impl.presentation.gifts.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_gift_impl.databinding.ItemGiftsHeaderBinding

class GiftsHeaderAdapter(
    private val handler: ItemHandler
) : SingleItemAdapter<GiftsHeaderHolder>(isShownByDefault = true) {

    interface ItemHandler {
        fun onLearnMoreClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiftsHeaderHolder {
        return GiftsHeaderHolder(
            ItemGiftsHeaderBinding.inflate(parent.inflater(), parent, false),
            handler
        )
    }

    override fun onBindViewHolder(holder: GiftsHeaderHolder, position: Int) {
    }
}

class GiftsHeaderHolder(binder: ItemGiftsHeaderBinding, handler: GiftsHeaderAdapter.ItemHandler) : RecyclerView.ViewHolder(binder.root) {

    init {
        binder.giftsHeaderLearnMore.setOnClickListener { handler.onLearnMoreClicked() }
    }
}
