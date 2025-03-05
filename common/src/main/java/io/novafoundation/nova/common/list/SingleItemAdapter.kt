package io.novafoundation.nova.common.list

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class SingleItemAdapter<T : ViewHolder>() : RecyclerView.Adapter<T>() {

    protected var showItem = false
        private set

    constructor(isShownByDefault: Boolean) : this() {
        showItem = isShownByDefault
    }

    override fun getItemCount(): Int {
        return if (showItem) 1 else 0
    }

    fun show(show: Boolean) {
        if (showItem != show) {
            showItem = show
            if (show) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }

    fun notifyChangedIfShown() {
        if (showItem) {
            notifyItemChanged(0)
        }
    }
}
