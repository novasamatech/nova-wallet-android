package io.novafoundation.nova.common.list

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild

class PlaceholderAdapter(@LayoutRes val layoutId: Int) : RecyclerView.Adapter<ShimmeringHolder>() {

    private var showPlaceholder = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmeringHolder {
        return ShimmeringHolder(parent.inflateChild(layoutId))
    }

    override fun onBindViewHolder(holder: ShimmeringHolder, position: Int) {}

    override fun getItemCount(): Int {
        return if (showPlaceholder) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return layoutId
    }

    fun showPlaceholder(show: Boolean) {
        if (showPlaceholder != show) {
            showPlaceholder = show
            if (show) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }
}

class ShimmeringHolder(view: View) : RecyclerView.ViewHolder(view)
