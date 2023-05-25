package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.view.GovernanceLocksModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel
import kotlinx.android.synthetic.main.item_referenda_header.view.governanceLocksDelegations
import kotlinx.android.synthetic.main.item_referenda_header.view.governanceLocksHeader
import kotlinx.android.synthetic.main.item_referenda_header.view.governanceLocksLocked
import kotlinx.android.synthetic.main.item_referenda_header.view.referendaAssetHeader
import kotlinx.android.synthetic.main.item_referenda_header.view.referendaHeaderFilter
import kotlinx.android.synthetic.main.item_referenda_header.view.referendaHeaderSearch

class ReferendaListHeaderAdapter(val imageLoader: ImageLoader, val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {
        fun onClickAssetSelector()

        fun onClickGovernanceLocks()

        fun onClickDelegations()

        fun onClickReferendaSearch()

        fun onClickReferendaFilters()
    }

    private var assetModel: AssetSelectorModel? = null
    private var locksModel: GovernanceLocksModel? = null
    private var delegationsModel: GovernanceLocksModel? = null
    private var filterIconRes: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, parent.inflateChild(R.layout.item_referenda_header), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(assetModel, locksModel, delegationsModel, filterIconRes)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.ASSET -> holder.bindAsset(assetModel)
                    Payload.LOCKS -> holder.bindLocks(locksModel)
                    Payload.DELEGATIONS -> holder.bindDelegations(delegationsModel)
                    Payload.FILTERS -> holder.bindFilters(filterIconRes)
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

    fun setDelegations(delegationsModel: GovernanceLocksModel?) {
        this.delegationsModel = delegationsModel
        notifyItemChanged(0, Payload.DELEGATIONS)
    }

    fun setFilterIcon(filterIconRes: Int) {
        this.filterIconRes = filterIconRes
        notifyItemChanged(0, Payload.FILTERS)
    }
}

private enum class Payload {
    ASSET, LOCKS, DELEGATIONS, FILTERS
}

class HeaderHolder(private val imageLoader: ImageLoader, view: View, handler: ReferendaListHeaderAdapter.Handler) : RecyclerView.ViewHolder(view) {

    init {
        with(view) {
            referendaAssetHeader.setOnClickListener { handler.onClickAssetSelector() }

            governanceLocksHeader.background = context.getBlockDrawable()

            governanceLocksLocked.setOnClickListener { handler.onClickGovernanceLocks() }
            governanceLocksDelegations.setOnClickListener { handler.onClickDelegations() }

            referendaHeaderSearch.setOnClickListener { handler.onClickReferendaSearch() }
            referendaHeaderFilter.setOnClickListener { handler.onClickReferendaFilters() }
        }
    }

    fun bind(
        assetModel: AssetSelectorModel?,
        locksModel: GovernanceLocksModel?,
        delegationsModel: GovernanceLocksModel?,
        filterIconRes: Int?
    ) {
        bindAsset(assetModel)
        bindLocks(locksModel)
        bindDelegations(delegationsModel)
        bindFilters(filterIconRes)
    }

    fun bindAsset(assetModel: AssetSelectorModel?) {
        assetModel?.let { itemView.referendaAssetHeader.setState(imageLoader, assetModel) }
    }

    fun bindLocks(locksModel: GovernanceLocksModel?) {
        itemView.governanceLocksLocked.letOrHide(locksModel) {
            itemView.governanceLocksLocked.setModel(it)
        }

        updateLocksContainerVisibility()
    }

    fun bindDelegations(model: GovernanceLocksModel?) {
        itemView.governanceLocksDelegations.letOrHide(model) {
            itemView.governanceLocksDelegations.setModel(it)
        }

        updateLocksContainerVisibility()
    }

    fun bindFilters(filterIconRes: Int?) {
        filterIconRes?.let { itemView.referendaHeaderFilter.setImageResource(filterIconRes) }
    }

    private fun updateLocksContainerVisibility() {
        val contentVisible = itemView.governanceLocksHeader.children.any { it.isVisible }

        itemView.governanceLocksHeader.setVisible(contentVisible)
    }
}
