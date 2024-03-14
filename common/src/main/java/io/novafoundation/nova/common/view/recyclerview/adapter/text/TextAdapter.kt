package io.novafoundation.nova.common.view.recyclerview.adapter.text

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.inflateChild
import kotlinx.android.synthetic.main.item_text.view.itemText

class TextAdapter(
    private var text: String? = null,
    @StyleRes private val styleRes: Int = R.style.TextAppearance_NovaFoundation_Bold_Title2
) : RecyclerView.Adapter<TextViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        return TextViewHolder(parent.inflateChild(R.layout.item_text))
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.bind(text?.let { TextRvItem(it) }, styleRes)
    }

    fun setText(text: String) {
        this.text = text
        notifyItemChanged(0)
    }
}

class TextViewHolder(view: View) : ViewHolder(view) {

    fun bind(item: TextRvItem?, styleRes: Int) {
        itemView.itemText.text = item?.text
        itemView.itemText.setTextAppearance(styleRes)
    }
}
