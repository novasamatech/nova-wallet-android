package io.novafoundation.nova.common.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


fun RecyclerView.onScrollPositionChanged(listener: (lastVisiblePosition: Int) -> Unit) {
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            listener(lastVisiblePosition)
        }
    }

    addOnScrollListener(scrollListener)
}
