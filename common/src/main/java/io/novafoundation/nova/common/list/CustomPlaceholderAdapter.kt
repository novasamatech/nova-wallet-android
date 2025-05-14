package io.novafoundation.nova.common.list

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild

open class CustomPlaceholderAdapter(@LayoutRes val layoutId: Int) : SingleItemAdapter<StubHolder>() {

    private var invisibleEnabled = false
        private set

    fun setInvisible(invisible: Boolean) {
        this.invisibleEnabled = invisible
        notifyChangedIfShown()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StubHolder {
        return StubHolder(parent.inflateChild(layoutId))
    }

    override fun onBindViewHolder(holder: StubHolder, position: Int) {
        holder.bind(invisibleEnabled)
    }

    override fun getItemViewType(position: Int): Int {
        return layoutId
    }
}

class StubHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(invisibleEnabled: Boolean) {
        itemView.isInvisible = invisibleEnabled
    }
}
