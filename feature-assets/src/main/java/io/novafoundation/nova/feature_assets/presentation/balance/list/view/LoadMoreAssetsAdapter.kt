package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.recyclerView.WithViewType
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemLoadMoreAssetsBinding

class LoadMoreAssetsAdapter(
    private val handler: Handler
) : SingleItemAdapter<LoadMoreAssetsHolder>() {

    interface Handler {
        fun loadMoreClicked()
    }

    @StringRes
    private var buttonTextRes: Int = R.string.assets_load_more_tokens

    fun setButtonText(@StringRes textRes: Int) {
        buttonTextRes = textRes
        notifyChangedIfShown()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadMoreAssetsHolder {
        val binder = ItemLoadMoreAssetsBinding.inflate(parent.inflater(), parent, false)
        return LoadMoreAssetsHolder(binder, handler)
    }

    override fun onBindViewHolder(holder: LoadMoreAssetsHolder, position: Int) {
        holder.bind(buttonTextRes)
    }

    override fun getItemViewType(position: Int): Int {
        return LoadMoreAssetsHolder.viewType
    }
}

class LoadMoreAssetsHolder(
    private val binder: ItemLoadMoreAssetsBinding,
    handler: LoadMoreAssetsAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    companion object : WithViewType {
        override val viewType: Int = R.layout.item_load_more_assets
    }

    init {
        binder.loadMoreAssetsButton.setOnClickListener { handler.loadMoreClicked() }
    }

    fun bind(@StringRes textRes: Int) {
        binder.loadMoreAssetsButton.setText(textRes)
    }
}
