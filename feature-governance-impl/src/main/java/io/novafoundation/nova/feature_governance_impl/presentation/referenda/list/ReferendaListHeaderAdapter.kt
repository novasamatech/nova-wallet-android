package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.view.GovernanceLocksModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel
import kotlinx.android.synthetic.main.item_referenda_header.view.governanceLocksHeader
import kotlinx.android.synthetic.main.item_referenda_header.view.referendaAssetHeader

class ReferendaListHeaderAdapter(val imageLoader: ImageLoader, val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {
        fun onClickAssetSelector()

        fun onClickGovernanceLocks()
    }

    private var assetModel: AssetSelectorModel? = null
    private var locksModel: GovernanceLocksModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, parent.inflateChild(R.layout.item_referenda_header), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(assetModel, locksModel)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.ASSET -> holder.bindAsset(assetModel)
                    Payload.LOCKS -> holder.bindLocks(locksModel)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setAsset(assetModel: AssetSelectorModel) {
        this.assetModel = assetModel
        notifyItemChanged(0, Payload.ASSET)
    }

    fun setLocks(locksModel: GovernanceLocksModel?) {
        this.locksModel = locksModel
        notifyItemChanged(0, Payload.LOCKS)
    }
}

private enum class Payload {
    ASSET, LOCKS
}

class HeaderHolder(private val imageLoader: ImageLoader, view: View, handler: ReferendaListHeaderAdapter.Handler) : RecyclerView.ViewHolder(view) {

    init {
        view.referendaAssetHeader.setOnClickListener { handler.onClickAssetSelector() }
        view.governanceLocksHeader.setOnClickListener { handler.onClickGovernanceLocks() }
    }

    fun bind(assetModel: AssetSelectorModel?, locksModel: GovernanceLocksModel?) {
        bindAsset(assetModel)
        bindLocks(locksModel)
    }

    fun bindAsset(assetModel: AssetSelectorModel?) {
        assetModel?.let { itemView.referendaAssetHeader.setState(imageLoader, assetModel) }
    }

    fun bindLocks(locksModel: GovernanceLocksModel?) {
        itemView.governanceLocksHeader.letOrHide(locksModel) {
            itemView.governanceLocksHeader.setModel(it)
        }
    }
}
