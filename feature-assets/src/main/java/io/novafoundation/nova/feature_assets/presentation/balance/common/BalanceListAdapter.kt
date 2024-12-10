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
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAdapter
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
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
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.databinding.ItemAssetBinding
import io.novafoundation.nova.feature_assets.databinding.ItemAssetGroupBinding
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private val priceRateExtractor = { asset: AssetModel -> asset.token.rate }
private val recentChangeExtractor = { asset: AssetModel -> asset.token.recentRateChange }
private val amountExtractor = { asset: AssetModel -> asset.amount }

private val tokenGroupPriceRateExtractor = { group: TokenGroupUi -> group.rate }
private val tokenGroupRecentChangeExtractor = { group: TokenGroupUi -> group.recentRateChange }
private val tokenGroupAmountExtractor = { group: TokenGroupUi -> group.balance }
private val tokenGroupTypeExtractor = { group: TokenGroupUi -> group.groupType }

const val TYPE_NETWORK_GROUP = 0
const val TYPE_NETWORK_ASSET = 1
const val TYPE_TOKEN_GROUP = 2
const val TYPE_TOKEN_ASSET = 3

class BalanceListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemAssetHandler,
) : ListAdapter<BalanceListRvItem, ViewHolder>(DiffCallback), ExpandableAdapter {

    interface ItemAssetHandler {
        fun assetClicked(asset: Chain.Asset)

        fun tokenGroupClicked(tokenGroup: TokenGroupUi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_NETWORK_GROUP -> NetworkAssetGroupViewHolder(parent.inflateChild(R.layout.item_network_asset_group)) //ItemAssetGroupBinding.inflate(parent.inflater(), parent, false)
            TYPE_NETWORK_ASSET -> NetworkAssetViewHolder(parent.inflateChild(R.layout.item_network_asset), imageLoader) //ItemAssetBinding.inflate(parent.inflater(), parent, false)
            TYPE_TOKEN_GROUP -> TokenAssetGroupViewHolder(parent.inflateChild(R.layout.item_token_asset_group), imageLoader, itemHandler)
            TYPE_TOKEN_ASSET -> TokenAssetViewHolder(parent.inflateChild(R.layout.item_token_asset), imageLoader)
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return when (holder) {
            is NetworkAssetGroupViewHolder -> holder.bind(getItem(position) as NetworkGroupUi)
            is NetworkAssetViewHolder -> holder.bind(getItem(position) as NetworkAssetUi, itemHandler)
            is TokenAssetGroupViewHolder -> holder.bind(getItem(position) as TokenGroupUi)
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
                        priceRateExtractor -> holder.bindPriceInfo(item.asset)
                        recentChangeExtractor -> holder.bindRecentChange(item.asset)
                        amountExtractor -> holder.bindTotal(item.asset)
                    }
                }
            }

            is TokenAssetViewHolder -> {
                val item = getItem(position) as TokenAssetUi
                resolvePayload(holder, position, payloads) {
                    when (it) {
                        amountExtractor -> holder.bindTotal(item.asset)
                    }
                }
            }

            is TokenAssetGroupViewHolder -> {
                val item = getItem(position) as TokenGroupUi
                resolvePayload(holder, position, payloads) {
                    when (it) {
                        tokenGroupPriceRateExtractor -> holder.bindPriceRate(item)
                        tokenGroupRecentChangeExtractor -> holder.bindRecentChange(item)
                        tokenGroupAmountExtractor -> holder.bindTotal(item)
                        tokenGroupTypeExtractor -> holder.bindGroupType(item)
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

    override fun getItems(): List<ExpandableBaseItem> {
        return currentList
    }
}

private object DiffCallback : DiffUtil.ItemCallback<BalanceListRvItem>() {

    override fun areItemsTheSame(oldItem: BalanceListRvItem, newItem: BalanceListRvItem): Boolean {
        return oldItem.itemId == newItem.itemId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: BalanceListRvItem, newItem: BalanceListRvItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: BalanceListRvItem, newItem: BalanceListRvItem): Any? {
        return when {
            oldItem is NetworkAssetUi && newItem is NetworkAssetUi -> NetworkAssetPayloadGenerator.diff(oldItem.asset, newItem.asset)

            oldItem is TokenAssetUi && newItem is TokenAssetUi -> TokenAssetPayloadGenerator.diff(oldItem.asset, newItem.asset)

            oldItem is TokenGroupUi && newItem is TokenGroupUi -> TokenGroupAssetPayloadGenerator.diff(oldItem, newItem)

            else -> null
        }
    }
}

private object NetworkAssetPayloadGenerator : PayloadGenerator<AssetModel>(
    priceRateExtractor,
    recentChangeExtractor,
    amountExtractor
)

private object TokenAssetPayloadGenerator : PayloadGenerator<AssetModel>(
    amountExtractor
)

private object TokenGroupAssetPayloadGenerator : PayloadGenerator<TokenGroupUi>(
    tokenGroupPriceRateExtractor,
    tokenGroupRecentChangeExtractor,
    tokenGroupAmountExtractor,
    tokenGroupTypeExtractor
)
