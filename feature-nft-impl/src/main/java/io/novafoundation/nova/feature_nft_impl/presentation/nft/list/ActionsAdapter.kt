package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_nft_impl.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_nft_list_actions.view.nftActionsReceive
import kotlinx.android.synthetic.main.item_nft_list_actions.view.nftActionsSend

class ActionsAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<ActionsHolder>() {

    interface Handler {

        fun sendClicked()

        fun receiveClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionsHolder {
        return ActionsHolder(parent.inflateChild(R.layout.item_nft_list_actions), handler)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: ActionsHolder, position: Int) {
    }
}

class ActionsHolder(
    override val containerView: View,
    private val itemHandler: ActionsAdapter.Handler
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            nftActionsSend.setOnClickListener { itemHandler.sendClicked() }
            nftActionsReceive.setOnClickListener { itemHandler.receiveClicked() }
        }
    }
}
