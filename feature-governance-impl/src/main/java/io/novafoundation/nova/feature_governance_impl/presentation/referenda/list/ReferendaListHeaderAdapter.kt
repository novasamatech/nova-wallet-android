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

class ReferendaListHeaderAdapter(val imageLoader: ImageLoader, val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {
        fun onClickAssetSelector()

        fun onClickGovernanceLocks()

        fun onClickDelegations()
    }

    private var assetModel: AssetSelectorModel? = null
    private var locksModel: GovernanceLocksModel? = null
    private var delegationsModel: GovernanceLocksModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, parent.inflateChild(R.layout.item_referenda_header), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(assetModel, locksModel, delegationsModel)
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
}

private enum class Payload {
    ASSET, LOCKS, DELEGATIONS
}

class HeaderHolder(private val imageLoader: ImageLoader, view: View, handler: ReferendaListHeaderAdapter.Handler) : RecyclerView.ViewHolder(view) {

    init {
        with(view) {
            referendaAssetHeader.setOnClickListener { handler.onClickAssetSelector() }

            governanceLocksHeader.background = context.getBlockDrawable()

            governanceLocksLocked.setOnClickListener { handler.onClickGovernanceLocks() }
            governanceLocksDelegations.setOnClickListener { handler.onClickDelegations() }
        }
    }

    fun bind(
        assetModel: AssetSelectorModel?,
        locksModel: GovernanceLocksModel?,
        delegationsModel: GovernanceLocksModel?
    ) {
        bindAsset(assetModel)
        bindLocks(locksModel)
        bindDelegations(delegationsModel)
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

    private fun updateLocksContainerVisibility() {
        val contentVisible = itemView.governanceLocksHeader.children.any { it.isVisible }

        itemView.governanceLocksHeader.setVisible(contentVisible)
    }
}
