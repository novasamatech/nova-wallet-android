package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.item_governance_tracks_header.view.selectGovernanceTracksChain

class SelectGovernanceTracksHeaderAdapter : RecyclerView.Adapter<SelectGovernanceTracksHeaderViewHolder>() {

    var chainUi: ChainUi? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectGovernanceTracksHeaderViewHolder {
        val containerView = parent.inflateChild(R.layout.item_governance_tracks_header)

        return SelectGovernanceTracksHeaderViewHolder(containerView)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: SelectGovernanceTracksHeaderViewHolder, position: Int) {
        holder.bind(chainUi)
    }

    fun setChain(chainUi: ChainUi) {
        this.chainUi = chainUi
    }
}

class SelectGovernanceTracksHeaderViewHolder(containerView: View) : ViewHolder(containerView) {

    fun bind(chainUi: ChainUi?) = with(itemView) {
        selectGovernanceTracksChain.setVisible(chainUi != null)
        chainUi?.let { selectGovernanceTracksChain.setChain(chainUi) }
    }
}
