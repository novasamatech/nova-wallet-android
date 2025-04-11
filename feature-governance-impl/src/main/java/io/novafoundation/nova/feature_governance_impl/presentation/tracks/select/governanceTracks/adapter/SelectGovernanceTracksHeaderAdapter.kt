package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_governance_impl.databinding.ItemGovernanceTracksHeaderBinding

class SelectGovernanceTracksHeaderAdapter : RecyclerView.Adapter<SelectGovernanceTracksHeaderViewHolder>() {

    var chainUi: ChainUi? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectGovernanceTracksHeaderViewHolder {
        return SelectGovernanceTracksHeaderViewHolder(ItemGovernanceTracksHeaderBinding.inflate(parent.inflater(), parent, false))
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

class SelectGovernanceTracksHeaderViewHolder(private val binder: ItemGovernanceTracksHeaderBinding) : ViewHolder(binder.root) {

    fun bind(chainUi: ChainUi?) = with(binder) {
        selectGovernanceTracksChain.setVisible(chainUi != null)
        chainUi?.let { selectGovernanceTracksChain.setChain(chainUi) }
    }
}
