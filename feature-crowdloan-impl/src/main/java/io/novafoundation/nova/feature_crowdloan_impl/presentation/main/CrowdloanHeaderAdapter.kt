package io.novafoundation.nova.feature_crowdloan_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull.StatefulCrowdloanMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel

class CrowdloanHeaderAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {
        fun onClickAssetSelector()

        fun onClickContributionsInfo()
    }

    private var assetModel: AssetSelectorModel? = null
    private var showShimmering: Boolean = false
    private var contributionsInfo: StatefulCrowdloanMixin.ContributionsInfo? = null
    private var aboutDescription: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, parent.inflateChild(R.layout.item_crowdloan_header), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(assetModel, contributionsInfo, showShimmering)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.ASSET -> holder.bindAsset(assetModel)
                    Payload.CONTRIBUTIONS -> holder.bindContributionsInfo(contributionsInfo, showShimmering)
                    Payload.ABOUT -> holder.bindAbout(aboutDescription)
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

    fun setContributionsInfo(contributionsInfo: StatefulCrowdloanMixin.ContributionsInfo?, showShimmering: Boolean) {
        this.contributionsInfo = contributionsInfo
        this.showShimmering = showShimmering
        notifyItemChanged(0, Payload.CONTRIBUTIONS)
    }

    fun setAboutDescription(about: String?) {
        this.aboutDescription = about
        notifyItemChanged(0, Payload.ABOUT)
    }
}

private enum class Payload {
    ASSET, CONTRIBUTIONS, ABOUT
}

class HeaderHolder(
    private val imageLoader: ImageLoader,
    view: View,
    handler: CrowdloanHeaderAdapter.Handler
) : RecyclerView.ViewHolder(view) {

    init {
        itemView.crowdloanAssetSelector.setOnClickListener { handler.onClickAssetSelector() }
        itemView.crowdloanTotalContributedContainer.setOnClickListener { handler.onClickContributionsInfo() }

        with(itemView) {
            crowdloanAbout.background = context.getBlockDrawable()
            crowdloanTotalContributedContainer.background = context.addRipple(context.getBlockDrawable())
            crowdloanTotalContributedShimmering.background = context.getBlockDrawable()
        }
    }

    fun bind(assetModel: AssetSelectorModel?, contributionsInfo: StatefulCrowdloanMixin.ContributionsInfo?, showShimmering: Boolean) {
        bindAsset(assetModel)
        bindContributionsInfo(contributionsInfo, showShimmering)
    }

    fun bindAsset(assetModel: AssetSelectorModel?) {
        assetModel?.let { itemView.crowdloanAssetSelector.setState(imageLoader, assetModel) }
    }

    fun bindContributionsInfo(contributionsInfo: StatefulCrowdloanMixin.ContributionsInfo?, showShimmering: Boolean) = with(itemView) {
        crowdloanTotalContributedShimmering.isVisible = showShimmering
        crowdloanAbout.isGone = showShimmering
        crowdloanTotalContributedContainer.isGone = showShimmering

        if (contributionsInfo != null && !showShimmering) {
            crowdloanTotalContributedContainer.isVisible = contributionsInfo.isUserHasContributions
            crowdloanAbout.isGone = contributionsInfo.isUserHasContributions
            crowdloanTotalContributionsCount.text = contributionsInfo.contributionsCount.format()
            crowdloanTotalContributedValue.text = contributionsInfo.totalContributed.token
            crowdloanTotalContributedFiat.text = contributionsInfo.totalContributed.fiat
        }
    }

    fun bindAbout(about: String?) {
        itemView.crowdloanMainDescription.text = about
    }
}
