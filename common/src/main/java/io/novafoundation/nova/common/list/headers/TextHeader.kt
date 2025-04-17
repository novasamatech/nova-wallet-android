package io.novafoundation.nova.common.list.headers

import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.databinding.ItemTextHeaderBinding
import io.novafoundation.nova.common.list.GroupedListHolder

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

class TextHeaderHolder(private val binder: ItemTextHeaderBinding) : GroupedListHolder(binder.root) {

    fun bind(item: TextHeader) {
        binder.textHeader.text = item.content
    }
}
