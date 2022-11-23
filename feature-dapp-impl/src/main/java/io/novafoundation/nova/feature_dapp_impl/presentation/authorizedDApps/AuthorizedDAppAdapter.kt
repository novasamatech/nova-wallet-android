package io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.model.AuthorizedDAppModel
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView

class AuthorizedDAppAdapter(
    private val handler: Handler
) : BaseListAdapter<AuthorizedDAppModel, AuthorizedDAppViewHolder>(DiffCallback) {

    interface Handler {

        fun onRevokeClicked(item: AuthorizedDAppModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorizedDAppViewHolder {
        return AuthorizedDAppViewHolder(DAppView.createUsingMathParentWidth(parent.context), handler)
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
    private val dAppView: DAppView,
    private val itemHandler: AuthorizedDAppAdapter.Handler,
) : BaseViewHolder(dAppView) {

    init {
        dAppView.setActionResource(R.drawable.ic_close)
        dAppView.setActionTintRes(R.color.icon_secondary)
    }

    fun bind(item: AuthorizedDAppModel) = with(dAppView) {
        this.setTitle(item.title)
        this.showTitle(item.title != null)
        this.setSubtitle(item.url)
        this.setIconUrl(item.iconLink)

        setOnActionClickListener { itemHandler.onRevokeClicked(item) }
    }

    override fun unbind() {
        dAppView.clearIcon()
    }
}
