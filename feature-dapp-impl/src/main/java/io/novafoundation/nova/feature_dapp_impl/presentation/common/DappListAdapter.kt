package io.novafoundation.nova.feature_dapp_impl.presentation.common

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView

class DappListAdapter(
    private val handler: DAppClickHandler
) : BaseListAdapter<DappModel, DappViewHolder>(DappModelDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappViewHolder {
        return DappViewHolder(DAppView.createUsingMathParentWidth(parent.context), handler)
    }

    override fun onBindViewHolder(holder: DappViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DappViewHolder(
    private val dAppView: DAppView,
    private val itemHandler: DAppClickHandler,
) : BaseViewHolder(dAppView) {

    fun bind(item: DappModel) = with(dAppView) {
        setTitle(item.name)
        setSubtitle(item.description)
        setIconUrl(item.iconUrl)
        setFavoriteIconVisible(item.isFavourite)

        setOnClickListener { itemHandler.onDAppClicked(item) }
    }

    override fun unbind() {
        dAppView.clearIcon()
    }
}
