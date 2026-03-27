package io.novafoundation.nova.feature_assets.presentation.tokens.manage.model

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

sealed class ManageTokensRvItem {

    data class Header(
        val id: String,
        val title: String,
        val icon: Icon?,
        val enabledCount: Int,
        val totalCount: Int,
        val isExpanded: Boolean,
    ) : ManageTokensRvItem()

    data class Child(
        val chainAssetId: FullChainAssetId,
        val name: String,
        val icon: Icon?,
        val isEnabled: Boolean,
        val isSwitchable: Boolean,
    ) : ManageTokensRvItem()
}

enum class ManageTokensTab {
    TOKENS,
    NETWORKS
}
