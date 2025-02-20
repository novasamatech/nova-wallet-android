package io.novafoundation.nova.common.list

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class SingleItemAdapter<T : ViewHolder>() : RecyclerView.Adapter<T>() {

    protected var showPlaceholder = false
        private set

    constructor(isShownByDefault: Boolean) : this() {
        showPlaceholder = isShownByDefault
    }

    override fun getItemCount(): Int {
        return if (showPlaceholder) 1 else 0
    }

    fun show(show: Boolean) {
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
