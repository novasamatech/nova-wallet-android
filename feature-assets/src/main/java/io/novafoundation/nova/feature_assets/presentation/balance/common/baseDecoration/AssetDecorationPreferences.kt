package io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_assets.presentation.balance.common.TYPE_NETWORK_GROUP
import io.novafoundation.nova.feature_assets.presentation.balance.common.TYPE_TOKEN_GROUP
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.NetworkAssetGroupViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.NetworkAssetViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.TokenAssetGroupViewHolder

interface AssetDecorationPreferences {

    fun innerGroupPadding(viewHolder: ViewHolder): Int

    fun outerGroupPadding(viewHolder: ViewHolder): Int

    fun isGroupItem(viewType: Int): Boolean

    fun shouldUseViewHolder(viewHolder: ViewHolder): Boolean
}

class NetworkAssetDecorationPreferences : AssetDecorationPreferences {

    override fun innerGroupPadding(viewHolder: ViewHolder): Int {
        return 8.dp(viewHolder.itemView.context)
    }

    override fun outerGroupPadding(viewHolder: ViewHolder): Int {
        return 8.dp(viewHolder.itemView.context)
    }

    override fun isGroupItem(viewType: Int): Boolean {
        return viewType == TYPE_NETWORK_GROUP
    }

    override fun shouldUseViewHolder(viewHolder: ViewHolder): Boolean {
        return viewHolder is NetworkAssetViewHolder ||
            viewHolder is NetworkAssetGroupViewHolder
    }
}

class TokenAssetGroupDecorationPreferences : AssetDecorationPreferences {

    override fun innerGroupPadding(viewHolder: ViewHolder): Int {
        return 0
    }

    override fun outerGroupPadding(viewHolder: ViewHolder): Int {
        return 8.dp(viewHolder.itemView.context)
    }

    override fun isGroupItem(viewType: Int): Boolean {
        return viewType == TYPE_TOKEN_GROUP
    }

    override fun shouldUseViewHolder(viewHolder: ViewHolder): Boolean {
        return viewHolder is TokenAssetGroupViewHolder
    }
}

class CompoundAssetDecorationPreferences(private vararg val preferences: AssetDecorationPreferences) : AssetDecorationPreferences {

    override fun innerGroupPadding(viewHolder: ViewHolder): Int {
        val firstPreferences = preferences.firstOrNull { it.shouldUseViewHolder(viewHolder) }
        return firstPreferences?.innerGroupPadding(viewHolder) ?: 0
    }

    override fun outerGroupPadding(viewHolder: ViewHolder): Int {
        val firstPreferences = preferences.firstOrNull { it.shouldUseViewHolder(viewHolder) }
        return firstPreferences?.outerGroupPadding(viewHolder) ?: 0
    }

    override fun isGroupItem(viewType: Int): Boolean {
        return preferences.any { it.isGroupItem(viewType) }
    }

    override fun shouldUseViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
        return preferences.any { it.shouldUseViewHolder(viewHolder) }
    }
}
