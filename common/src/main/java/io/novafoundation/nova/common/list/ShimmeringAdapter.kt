package io.novafoundation.nova.common.list

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild

class ShimmeringAdapter(@LayoutRes val layoutId: Int) : RecyclerView.Adapter<ShimmeringHolder>() {

    private var shouldShowShimmering = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmeringHolder {
        return ShimmeringHolder(parent.inflateChild(layoutId))
    }

    override fun onBindViewHolder(holder: ShimmeringHolder, position: Int) {}

    override fun getItemCount(): Int {
        return if (shouldShowShimmering) 1 else 0
    }

    fun showShimmering(show: Boolean) {
        if (shouldShowShimmering != show) {
            shouldShowShimmering = show
            if (show) {
                notifyItemInserted(1)
            } else {
                notifyItemRemoved(1)
            }
        }
    }
}

class ShimmeringHolder(view: View) : RecyclerView.ViewHolder(view)
