package io.novafoundation.nova.feature_assets.presentation.tokens.manage.model

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * Token and network icons come from different sources and carry different placeholders, so the
 * row model says which one it holds rather than making the view holder guess from the view mode.
 */
sealed class ManageAssetIconModel {

    data class Token(val icon: Icon) : ManageAssetIconModel()

    data class Network(val url: String?) : ManageAssetIconModel()
}

/**
 * A flat row of the manage tokens list: groups, each followed by its own rows while expanded.
 *
 * The rows carry the shared expandable contract so that the same animator the balance list uses
 * can slide children in and out of their group.
 */
sealed class ManageAssetsRvItem : ExpandableBaseItem {

    // Named `key` rather than `id` to stay off the JVM signature of ExpandableBaseItem.getId()
    abstract val key: String

    override fun getId(): String = key

    /**
     * Section title. While searching, the sections collapse into a single result count - which
     * section a match belongs to is not what the user is looking at then.
     */
    data class Header(
        override val key: String,
        val title: String
    ) : ManageAssetsRvItem()

    data class Group(
        override val key: String,
        val icon: ManageAssetIconModel,
        val title: String,
        val subtitle: String,
        val enabled: Boolean,
        val switchable: Boolean,
        val expanded: Boolean,
        /**
         * A group holding a single asset has nothing to disclose, so the design hides its chevron
         */
        val expandable: Boolean
    ) : ManageAssetsRvItem(), ExpandableParentItem

    data class Child(
        override val key: String,
        override val groupId: String,
        val assetId: FullChainAssetId,
        val icon: ManageAssetIconModel,
        val title: String,
        val subtitle: String?,
        val enabled: Boolean,
        val switchable: Boolean
    ) : ManageAssetsRvItem(), ExpandableChildItem
}
