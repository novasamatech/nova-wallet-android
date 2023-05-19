package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.model.SessionListModel

class WalletConnectSessionsAdapter(
    private val handler: Handler
) : BaseListAdapter<SessionListModel, SessionHolder>(SessionDiffCallback()) {

    interface Handler {

        fun itemClicked(item: SessionListModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionHolder {
        return SessionHolder(DAppView.createUsingMathParentWidth(parent.context), handler)
    }

    override fun onBindViewHolder(holder: SessionHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SessionHolder(
    private val dAppView: DAppView,
    private val itemHandler: WalletConnectSessionsAdapter.Handler,
) : BaseViewHolder(dAppView) {

    override fun unbind() {
        dAppView.clearIcon()
    }

    fun bind(item: SessionListModel) = with(dAppView) {
        setIconUrl(item.iconUrl)
        setTitle(item.dappTitle)
        setSubtitle(item.walletModel.name)
        enableSubtitleIcon().setImageDrawable(item.walletModel.icon)

        setActionResource(iconRes = R.drawable.ic_chevron_right, colorRes = R.color.icon_secondary)

        setOnClickListener { itemHandler.itemClicked(item) }
    }
}

class SessionDiffCallback : DiffUtil.ItemCallback<SessionListModel>() {

    override fun areItemsTheSame(oldItem: SessionListModel, newItem: SessionListModel): Boolean {
        return oldItem.sessionTopic == newItem.sessionTopic
    }

    override fun areContentsTheSame(oldItem: SessionListModel, newItem: SessionListModel): Boolean {
        return oldItem == newItem
    }
}
