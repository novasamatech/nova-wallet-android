package io.novafoundation.nova.feature_assets.presentation.balance.common

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.NetworkAssetGroupViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.NetworkAssetViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.TokenAssetGroupViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.TokenAssetViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkGroupUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

private val priceRateExtractor = { assetModel: NetworkAssetUi -> assetModel.asset.token.rate }
private val recentChangeExtractor = { assetModel: NetworkAssetUi -> assetModel.asset.token.recentRateChange }
private val amountExtractor = { assetModel: NetworkAssetUi -> assetModel.asset.amount }

const val TYPE_NETWORK_GROUP = 0
const val TYPE_NETWORK_ASSET = 1
const val TYPE_TOKEN_GROUP = 2
const val TYPE_TOKEN_ASSET = 3

class BalanceListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemAssetHandler,
) : ListAdapter<BalanceListRvItem, ViewHolder>(DiffCallback) {

    interface ItemAssetHandler {
        fun assetClicked(asset: AssetModel)

        fun tokenGroupClicked(tokenGroup: TokenGroupUi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_NETWORK_GROUP -> NetworkAssetGroupViewHolder(parent.inflateChild(R.layout.item_network_asset_group))
            TYPE_NETWORK_ASSET -> NetworkAssetViewHolder(parent.inflateChild(R.layout.item_network_asset), imageLoader)
            TYPE_TOKEN_GROUP -> TokenAssetGroupViewHolder(parent.inflateChild(R.layout.item_token_asset_group), imageLoader)
            TYPE_TOKEN_ASSET -> TokenAssetViewHolder(parent.inflateChild(R.layout.item_token_asset), imageLoader)
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return when (holder) {
            is NetworkAssetGroupViewHolder -> holder.bind(getItem(position) as NetworkGroupUi)
            is NetworkAssetViewHolder -> holder.bind(getItem(position) as NetworkAssetUi, itemHandler)
            is TokenAssetGroupViewHolder -> holder.bind(getItem(position) as TokenGroupUi, itemHandler)
            is TokenAssetViewHolder -> holder.bind(getItem(position) as TokenAssetUi, itemHandler)
            else -> error("Unknown holder")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        when (holder) {
            is NetworkAssetViewHolder -> {
                val item = getItem(position) as NetworkAssetUi
                resolvePayload(holder, position, payloads) {
                    when (it) {
                        priceRateExtractor -> holder.bindPriceInfo(item)
                        recentChangeExtractor -> holder.bindRecentChange(item)
                        AssetModel::amount -> holder.bindTotal(item)
                    }
                }
            }

            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NetworkGroupUi -> TYPE_NETWORK_GROUP
            is NetworkAssetUi -> TYPE_NETWORK_ASSET
            is TokenGroupUi -> TYPE_TOKEN_GROUP
            is TokenAssetUi -> TYPE_TOKEN_ASSET
            else -> error("Unknown item type")
        }
    }
}

private object DiffCallback : DiffUtil.ItemCallback<BalanceListRvItem>() {

    override fun areItemsTheSame(oldItem: BalanceListRvItem, newItem: BalanceListRvItem): Boolean {
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: BalanceListRvItem, newItem: BalanceListRvItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: BalanceListRvItem, newItem: BalanceListRvItem): Any? {
        if (oldItem is NetworkAssetUi && newItem is NetworkAssetUi) {
            return AssetPayloadGenerator.diff(oldItem, newItem)
        }

        return null
    }
}

private object AssetPayloadGenerator : PayloadGenerator<NetworkAssetUi>(
    priceRateExtractor,
    recentChangeExtractor,
    amountExtractor
)
