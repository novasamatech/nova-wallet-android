package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.gradientDrawable
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import kotlinx.android.synthetic.main.item_asset.view.itemAssetBalance
import kotlinx.android.synthetic.main.item_asset.view.itemAssetDollarAmount
import kotlinx.android.synthetic.main.item_asset.view.itemAssetImage
import kotlinx.android.synthetic.main.item_asset.view.itemAssetRate
import kotlinx.android.synthetic.main.item_asset.view.itemAssetRateChange
import kotlinx.android.synthetic.main.item_asset.view.itemAssetToken
import kotlinx.android.synthetic.main.item_asset_group.view.itemAssetGroupBalance
import kotlinx.android.synthetic.main.item_asset_group.view.itemAssetGroupLabel
import kotlinx.android.synthetic.main.item_asset_group.view.itemAssetGroupLabelIcon
import java.math.BigDecimal

val dollarRateExtractor = { assetModel: AssetModel -> assetModel.token.dollarRate }
val recentChangeExtractor = { assetModel: AssetModel -> assetModel.token.recentRateChange }

class BalanceListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemAssetHandler,
) : GroupedListAdapter<AssetGroupUi, AssetModel>(DiffCallback) {

    interface ItemAssetHandler {
        fun assetClicked(asset: AssetModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_asset_group)

        return AssetGroupViewHolder(view, imageLoader)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_asset)

        return AssetViewHolder(view, imageLoader)
    }

    override fun bindGroup(holder: GroupedListHolder, group: AssetGroupUi) {
        require(holder is AssetGroupViewHolder)

        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: AssetModel, payloads: List<Any>) {
        require(holder is AssetViewHolder)

        resolvePayload(holder, position, payloads) {
            when (it) {
                dollarRateExtractor -> holder.bindDollarInfo(child)
                recentChangeExtractor -> holder.bindRecentChange(child)
                AssetModel::total -> holder.bindTotal(child)
            }
        }
    }

    override fun bindChild(holder: GroupedListHolder, child: AssetModel) {
        require(holder is AssetViewHolder)

        holder.bind(child, itemHandler)
    }
}

class AssetGroupViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader
): GroupedListHolder(containerView) {
    init {
        with(containerView) {
            itemAssetGroupLabel.background = context.getRoundedCornerDrawable(R.color.white_16, cornerSizeInDp = 8)
        }
    }

    fun bind(assetGroup: AssetGroupUi) = with(containerView) {

        itemAssetGroupLabel.text = assetGroup.chainUi.name
        itemAssetGroupBalance.text = assetGroup.groupBalanceFiat

        itemAssetGroupLabelIcon.load(assetGroup.chainUi.icon, imageLoader)
        itemAssetGroupLabelIcon.background = context.gradientDrawable(
            colors = assetGroup.chainUi.gradient.colors,
            offsets = assetGroup.chainUi.gradient.positions,
            angle = assetGroup.chainUi.gradient.angle,
            cornerRadiusDp = 8
        )
    }
}

class AssetViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(containerView) {

    fun bind(asset: AssetModel, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        itemAssetImage.load(asset.token.configuration.iconUrl, imageLoader)

        bindDollarInfo(asset)

        bindRecentChange(asset)

        bindTotal(asset)

        itemAssetToken.text = asset.token.configuration.symbol

        setOnClickListener { itemHandler.assetClicked(asset) }
    }

    fun bindTotal(asset: AssetModel) {
        containerView.itemAssetBalance.text = asset.total.format()

        bindDollarAmount(asset.dollarAmount)
    }

    fun bindRecentChange(asset: AssetModel) = with(containerView) {
        itemAssetRateChange.setTextColorRes(asset.token.rateChangeColorRes)
        itemAssetRateChange.text = asset.token.recentRateChange
    }

    fun bindDollarInfo(asset: AssetModel) = with(containerView) {
        itemAssetRate.text = asset.token.dollarRate
        bindDollarAmount(asset.dollarAmount)
    }

    private fun bindDollarAmount(dollarAmount: BigDecimal?) {
        containerView.itemAssetDollarAmount.text = dollarAmount?.formatAsCurrency()
    }
}

private object DiffCallback: BaseGroupedDiffCallback<AssetGroupUi, AssetModel>(AssetGroupUi::class.java) {

    override fun areGroupItemsTheSame(oldItem: AssetGroupUi, newItem: AssetGroupUi): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areGroupContentsTheSame(oldItem: AssetGroupUi, newItem: AssetGroupUi): Boolean {
       return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: AssetModel, newItem: AssetModel): Boolean {
        return oldItem.token.configuration == newItem.token.configuration
    }

    override fun areChildContentsTheSame(oldItem: AssetModel, newItem: AssetModel): Boolean {
        return  oldItem == newItem
    }

    override fun getChildChangePayload(oldItem: AssetModel, newItem: AssetModel): Any? {
        return AssetPayloadGenerator.diff(oldItem, newItem)
    }
}

private object AssetPayloadGenerator : PayloadGenerator<AssetModel>(
    dollarRateExtractor, recentChangeExtractor, AssetModel::total
)
