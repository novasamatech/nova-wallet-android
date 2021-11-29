package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_asset.view.itemAssetBalance
import kotlinx.android.synthetic.main.item_asset.view.itemAssetContainer
import kotlinx.android.synthetic.main.item_asset.view.itemAssetDollarAmount
import kotlinx.android.synthetic.main.item_asset.view.itemAssetImage
import kotlinx.android.synthetic.main.item_asset.view.itemAssetNetwork
import kotlinx.android.synthetic.main.item_asset.view.itemAssetRate
import kotlinx.android.synthetic.main.item_asset.view.itemAssetRateChange
import kotlinx.android.synthetic.main.item_asset.view.itemAssetToken
import java.math.BigDecimal

val dollarRateExtractor = { assetModel: AssetModel -> assetModel.token.dollarRate }
val recentChangeExtractor = { assetModel: AssetModel -> assetModel.token.recentRateChange }

class BalanceListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemAssetHandler,
) : ListAdapter<AssetModel, AssetViewHolder>(AssetDiffCallback) {

    interface ItemAssetHandler {
        fun assetClicked(asset: AssetModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = parent.inflateChild(R.layout.item_asset)

        return AssetViewHolder(view, imageLoader)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item, itemHandler)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                dollarRateExtractor -> holder.bindDollarInfo(item)
                recentChangeExtractor -> holder.bindRecentChange(item)
                AssetModel::total -> holder.bindTotal(item)
            }
        }
    }
}

class AssetViewHolder(
    override val containerView: View,
    private val imageLoader: ImageLoader,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        with(containerView) {
            val background = with(context) {
                addRipple(getRoundedCornerDrawable(R.color.blurColor))
            }

            containerView.itemAssetContainer.background = background
        }
    }

    fun bind(asset: AssetModel, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        itemAssetImage.load(asset.token.configuration.iconUrl, imageLoader)
        itemAssetNetwork.text = asset.token.configuration.name

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

private object AssetDiffCallback : DiffUtil.ItemCallback<AssetModel>() {

    override fun areItemsTheSame(oldItem: AssetModel, newItem: AssetModel): Boolean {
        return oldItem.token.configuration == newItem.token.configuration
    }

    override fun areContentsTheSame(oldItem: AssetModel, newItem: AssetModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: AssetModel, newItem: AssetModel): Any? {
        return AssetPayloadGenerator.diff(oldItem, newItem)
    }
}

private object AssetPayloadGenerator : PayloadGenerator<AssetModel>(
    dollarRateExtractor, recentChangeExtractor, AssetModel::total
)
