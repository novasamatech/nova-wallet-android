package io.novafoundation.nova.common.list

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild

class CustomPlaceholderAdapter(@LayoutRes val layoutId: Int) : PlaceholderAdapter<StubHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StubHolder {
        return StubHolder(parent.inflateChild(layoutId))
    }

    override fun onBindViewHolder(holder: StubHolder, position: Int) {}

    override fun getItemViewType(position: Int): Int {
        return layoutId
    }
}

class StubHolder(view: View) : RecyclerView.ViewHolder(view)
