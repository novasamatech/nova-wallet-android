package io.novafoundation.nova.feature_assets.presentation.balance.common

import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.databinding.ItemAssetBinding
import io.novafoundation.nova.feature_assets.databinding.ItemAssetGroupBinding
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

val priceRateExtractor = { assetModel: AssetModel -> assetModel.token.rate }
val recentChangeExtractor = { assetModel: AssetModel -> assetModel.token.recentRateChange }

class BalanceListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemAssetHandler,
) : GroupedListAdapter<AssetGroupUi, AssetModel>(DiffCallback) {

    interface ItemAssetHandler {
        fun assetClicked(asset: AssetModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return AssetGroupViewHolder(ItemAssetGroupBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return AssetViewHolder(ItemAssetBinding.inflate(parent.inflater(), parent, false), imageLoader)
    }

    override fun bindGroup(holder: GroupedListHolder, group: AssetGroupUi) {
        require(holder is AssetGroupViewHolder)

        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: AssetModel, payloads: List<Any>) {
        require(holder is AssetViewHolder)

        resolvePayload(holder, position, payloads) {
            when (it) {
                priceRateExtractor -> holder.bindPriceInfo(child)
                recentChangeExtractor -> holder.bindRecentChange(child)
                AssetModel::amount -> holder.bindTotal(child)
            }
        }
    }

    override fun bindChild(holder: GroupedListHolder, child: AssetModel) {
        require(holder is AssetViewHolder)

        holder.bind(child, itemHandler)
    }
}

class AssetGroupViewHolder(
    private val binder: ItemAssetGroupBinding,
) : GroupedListHolder(binder.root) {

    fun bind(assetGroup: AssetGroupUi) = with(binder) {
        itemAssetGroupChain.setChain(assetGroup.chainUi)
        itemAssetGroupBalance.text = assetGroup.groupBalanceFiat
    }
}

class AssetViewHolder(
    private val binder: ItemAssetBinding,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(binder.root) {

    fun bind(asset: AssetModel, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(binder) {
        itemAssetImage.loadTokenIcon(asset.token.configuration.iconUrl, imageLoader)

        bindPriceInfo(asset)

        bindRecentChange(asset)

        bindTotal(asset)

        itemAssetToken.text = asset.token.configuration.symbol.value

        binder.root.setOnClickListener { itemHandler.assetClicked(asset) }
    }

    fun bindTotal(asset: AssetModel) {
        binder.itemAssetBalance.text = asset.amount.token
        binder.itemAssetPriceAmount.text = asset.amount.fiat
    }

    fun bindRecentChange(asset: AssetModel) = with(binder) {
        itemAssetRateChange.setTextColorRes(asset.token.rateChangeColorRes)
        itemAssetRateChange.text = asset.token.recentRateChange
    }

    fun bindPriceInfo(asset: AssetModel) = with(binder) {
        itemAssetRate.text = asset.token.rate
    }
}

private object DiffCallback : BaseGroupedDiffCallback<AssetGroupUi, AssetModel>(AssetGroupUi::class.java) {

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
        return oldItem == newItem
    }

    override fun getChildChangePayload(oldItem: AssetModel, newItem: AssetModel): Any? {
        return AssetPayloadGenerator.diff(oldItem, newItem)
    }
}

private object AssetPayloadGenerator : PayloadGenerator<AssetModel>(
    priceRateExtractor,
    recentChangeExtractor,
    AssetModel::amount
)
