package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.recyclerview.widget.DiffUtil

data class AssetSelectorModel(
    val chainId: String,
    val chainAssetId: Int,
    val imageUrl: String,
    val tokenName: String,
    val assetBalance: String
) {

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssetSelectorModel>() {

            override fun areItemsTheSame(oldItem: AssetSelectorModel, newItem: AssetSelectorModel): Boolean {
                return oldItem.chainId == newItem.chainId && oldItem.chainAssetId == newItem.chainAssetId
            }

            override fun areContentsTheSame(oldItem: AssetSelectorModel, newItem: AssetSelectorModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
