package io.novafoundation.nova.common.view.recyclerview.adapter.text

import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.updatePadding

class TextAdapter(
    private var text: String? = null,
    @StyleRes private val styleRes: Int = R.style.TextAppearance_NovaFoundation_Bold_Title2,
    @ColorRes private val textColor: Int? = R.color.text_primary,
    private val paddingInDp: ViewSpace? = null,
    private var isShown: Boolean = true
) : RecyclerView.Adapter<TextViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        return TextViewHolder(parent.inflateChild(R.layout.item_text))
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.bind(text, styleRes, textColor, paddingInDp, isShown)
    }

    fun setText(text: String) {
        this.text = text
        notifyItemChanged(0)
    }

    fun show(show: Boolean) {
        isShown = show
        notifyItemChanged(0)
    }
}

class TextViewHolder(view: View) : ViewHolder(view) {

    fun bind(text: String?, styleRes: Int, textColor: Int?, paddingInDp: ViewSpace?, isShown: Boolean) {
        itemView.itemText.isVisible = isShown
        itemView.itemText.text = text
        itemView.itemText.setTextAppearance(styleRes)
        textColor?.let { itemView.itemText.setTextColor(itemView.context.getColor(it)) }
        paddingInDp?.let { itemView.updatePadding(it.dp(itemView.context)) }
    }
}
