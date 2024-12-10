package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_governance_impl.databinding.ItemReferendaHeaderBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.TinderGovBannerModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.GovernanceLocksModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.setTextOrHide
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel

class ReferendaListHeaderAdapter(val imageLoader: ImageLoader, val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {
        fun onClickAssetSelector()

        fun onClickGovernanceLocks()

        fun onClickDelegations()

        fun onClickReferendaSearch()

        fun onClickReferendaFilters()

        fun onTinderGovBannerClicked()
    }

    private var assetModel: AssetSelectorModel? = null
    private var locksModel: GovernanceLocksModel? = null
    private var delegationsModel: GovernanceLocksModel? = null
    private var tinderGovBannerModel: TinderGovBannerModel? = null
    private var filterIconRes: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, ItemReferendaHeaderBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(assetModel, locksModel, delegationsModel, tinderGovBannerModel, filterIconRes)
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
                    Payload.TINDER_GOV -> holder.bindTinderGovBanner(tinderGovBannerModel)
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

    fun setTinderGovBanner(tinderGovBannerModel: TinderGovBannerModel?) {
        this.tinderGovBannerModel = tinderGovBannerModel
        notifyItemChanged(0, Payload.TINDER_GOV)
    }

    fun setFilterIcon(filterIconRes: Int) {
        this.filterIconRes = filterIconRes
        notifyItemChanged(0, Payload.FILTERS)
    }
}

private enum class Payload {
    ASSET, LOCKS, DELEGATIONS, TINDER_GOV, FILTERS
}

class HeaderHolder(
    private val imageLoader: ImageLoader,
    private val binder: ItemReferendaHeaderBinding,
    handler: ReferendaListHeaderAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    init {
        with(binder) {
            referendaAssetHeader.setOnClickListener { handler.onClickAssetSelector() }

            governanceLocksHeader.background = binder.root.context.getBlockDrawable()

            governanceLocksLocked.setOnClickListener { handler.onClickGovernanceLocks() }
            governanceLocksDelegations.setOnClickListener { handler.onClickDelegations() }

            referendaHeaderSearch.setOnClickListener { handler.onClickReferendaSearch() }
            referendaHeaderFilter.setOnClickListener { handler.onClickReferendaFilters() }
            referendaTindergovBanner.setOnClickListener { handler.onTinderGovBannerClicked() }
        }
    }

    fun bind(
        assetModel: AssetSelectorModel?,
        locksModel: GovernanceLocksModel?,
        delegationsModel: GovernanceLocksModel?,
        tinderGovBannerModel: TinderGovBannerModel?,
        filterIconRes: Int?
    ) {
        bindAsset(assetModel)
        bindLocks(locksModel)
        bindDelegations(delegationsModel)
        bindTinderGovBanner(tinderGovBannerModel)
        bindFilters(filterIconRes)
    }

    fun bindAsset(assetModel: AssetSelectorModel?) {
        assetModel?.let { binder.referendaAssetHeader.setState(imageLoader, assetModel) }
    }

    fun bindLocks(locksModel: GovernanceLocksModel?) {
        binder.governanceLocksLocked.letOrHide(locksModel) {
            binder.governanceLocksLocked.setModel(it)
        }

        updateLocksContainerVisibility()
    }

    fun bindDelegations(model: GovernanceLocksModel?) {
        binder.governanceLocksDelegations.letOrHide(model) {
            binder.governanceLocksDelegations.setModel(it)
        }

        updateLocksContainerVisibility()
    }

    fun bindTinderGovBanner(model: TinderGovBannerModel?) {
        binder.referendaTindergovBanner.letOrHide(model) {
            binder.referendaTinderGovChip.setTextOrHide(it.chipText)
        }

        updateLocksContainerVisibility()
    }

    fun bindFilters(filterIconRes: Int?) {
        filterIconRes?.let { binder.referendaHeaderFilter.setImageResource(filterIconRes) }
    }

    private fun updateLocksContainerVisibility() {
        val contentVisible = binder.governanceLocksHeader.children.any { it.isVisible }

        binder.governanceLocksHeader.setVisible(contentVisible)
    }
}
