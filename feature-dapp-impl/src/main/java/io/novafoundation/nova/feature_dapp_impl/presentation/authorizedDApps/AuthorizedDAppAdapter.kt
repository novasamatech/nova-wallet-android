package io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.model.AuthorizedDAppModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.showDAppIcon
import kotlinx.android.synthetic.main.item_dapp.view.itemDAppIcon
import kotlinx.android.synthetic.main.item_dapp.view.itemDAppSubtitle
import kotlinx.android.synthetic.main.item_dapp.view.itemDAppTitle
import kotlinx.android.synthetic.main.item_dapp.view.itemDappAction

class AuthorizedDAppAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader,
) : BaseListAdapter<AuthorizedDAppModel, AuthorizedDAppViewHolder>(DiffCallback) {

    interface Handler {

        fun onRevokeClicked(item: AuthorizedDAppModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorizedDAppViewHolder {
        return AuthorizedDAppViewHolder(parent.inflateChild(R.layout.item_dapp), handler, imageLoader)
    }

    override fun onBindViewHolder(holder: AuthorizedDAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DiffCallback : DiffUtil.ItemCallback<AuthorizedDAppModel>() {

    override fun areItemsTheSame(oldItem: AuthorizedDAppModel, newItem: AuthorizedDAppModel): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: AuthorizedDAppModel, newItem: AuthorizedDAppModel): Boolean {
        return oldItem == newItem
    }
}

class AuthorizedDAppViewHolder(
    containerView: View,
    private val itemHandler: AuthorizedDAppAdapter.Handler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(containerView) {

    init {
        containerView.itemDappAction.setImageResource(R.drawable.ic_close)
        containerView.itemDappAction.setImageTintRes(R.color.white_48)
    }

    fun bind(item: AuthorizedDAppModel) = with(containerView) {
        itemDAppIcon.showDAppIcon(item.iconLink, imageLoader)
        itemDAppTitle.setTextOrHide(item.title)
        itemDAppSubtitle.text = item.url

        itemDappAction.setOnClickListener { itemHandler.onRevokeClicked(item) }
    }

    override fun unbind() {
        containerView.itemDAppIcon.clear()
    }
}
