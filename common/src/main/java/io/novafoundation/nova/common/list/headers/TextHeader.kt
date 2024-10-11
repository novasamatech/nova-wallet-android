package io.novafoundation.nova.common.list.headers

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild

class TextHeader(val content: String) {

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TextHeader>() {

            override fun areItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
                return oldItem.content == newItem.content
            }

            override fun areContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
                return true
            }
        }
    }
}

class TextHeaderHolder(parentView: ViewGroup) : GroupedListHolder(parentView.inflateChild(R.layout.item_text_header)) {

    fun bind(item: TextHeader) {
        containerView.textHeader.text = item.content
    }
}
