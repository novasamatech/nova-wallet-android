package io.novafoundation.nova.common.view.recyclerview.adapter.text

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.inflateChild
import kotlinx.android.synthetic.main.item_text.view.itemText

class TextAdapter(
    initText: String? = null,
    @StyleRes private val styleRes: Int = R.style.TextAppearance_NovaFoundation_Bold_Title2
) : ListAdapter<TextRvItem, TextViewHolder>(TextAdapterDiffCallback()) {

    init {
        if (initText != null) {
            submitList(listOf(TextRvItem(initText)))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        return TextViewHolder(parent.inflateChild(R.layout.item_text))
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.bind(getItem(position), styleRes)
    }

    fun setText(text: String) {
        submitList(listOf(TextRvItem(text)))
    }
}

class TextViewHolder(view: View) : ViewHolder(view) {

    fun bind(item: TextRvItem, styleRes: Int) {
        itemView.itemText.text = item.text
        itemView.itemText.setTextAppearance(styleRes)
    }
}

class TextAdapterDiffCallback : DiffUtil.ItemCallback<TextRvItem>() {

    override fun areItemsTheSame(oldItem: TextRvItem, newItem: TextRvItem): Boolean {
        return true
    }

    override fun areContentsTheSame(oldItem: TextRvItem, newItem: TextRvItem): Boolean {
        return oldItem.text == newItem.text
    }
}
