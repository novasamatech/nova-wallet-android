package io.novafoundation.nova.feature_assets.presentation.tokens.manage.model

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.formatListPreview
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageAssetGroup
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageAssetItem

class ManageAssetsMapper(
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager
) {

    fun mapGroupsToUi(
        groups: List<ManageAssetGroup>,
        expandedGroupIds: Set<String>,
        searching: Boolean
    ): List<ManageAssetsRvItem> {
        return buildList {
            var previousSection: Boolean? = null

            if (searching) {
                add(searchResultsHeader(groups.size))
            }

            groups.forEach { group ->
                if (!searching && group.isDefault != previousSection) {
                    add(sectionHeader(group.isDefault))
                    previousSection = group.isDefault
                }

                val expanded = group.id in expandedGroupIds
                add(mapGroupToUi(group, expanded))

                if (expanded) {
                    group.items.forEach { add(mapItemToUi(it, group)) }
                }
            }
        }
    }

    private fun searchResultsHeader(resultCount: Int): ManageAssetsRvItem.Header {
        return ManageAssetsRvItem.Header(
            key = "search_results",
            title = resourceManager.getQuantityString(R.plurals.assets_manage_tokens_search_results, resultCount, resultCount)
        )
    }

    private fun sectionHeader(default: Boolean): ManageAssetsRvItem.Header {
        val title = if (default) R.string.assets_manage_tokens_section_default else R.string.assets_manage_tokens_section_other

        return ManageAssetsRvItem.Header(
            key = if (default) "section_default" else "section_other",
            title = resourceManager.getString(title)
        )
    }

    private fun mapGroupToUi(group: ManageAssetGroup, expanded: Boolean): ManageAssetsRvItem.Group {
        return ManageAssetsRvItem.Group(
            key = group.id,
            icon = iconFor(group.kind, group.iconUrl),
            title = group.title,
            subtitle = constructSubtitle(group),
            enabled = group.isEnabled,
            switchable = group.isSwitchable,
            expanded = expanded,
            expandable = group.items.size > 1
        )
    }

    private fun mapItemToUi(item: ManageAssetItem, group: ManageAssetGroup): ManageAssetsRvItem.Child {
        return ManageAssetsRvItem.Child(
            // Scoped by group: the same network appears under every token it hosts
            key = "${group.id}:${item.id.chainId}:${item.id.assetId}",
            groupId = group.id,
            assetId = item.id,
            icon = iconFor(group.kind.inner(), item.iconUrl),
            title = item.title,
            subtitle = item.subtitle,
            enabled = item.isEnabled,
            switchable = item.isSwitchable
        )
    }

    private fun constructSubtitle(group: ManageAssetGroup): String {
        val enabledTitles = group.items.filter { it.isEnabled }.map { it.title }

        return if (enabledTitles.size == group.items.size) {
            resourceManager.getString(group.kind.inner().allLabel())
        } else {
            resourceManager.formatListPreview(enabledTitles, zeroLabel = R.string.common_disabled)
        }
    }

    private fun iconFor(kind: ManageAssetGroup.Kind, url: String?): ManageAssetIconModel {
        return when (kind) {
            ManageAssetGroup.Kind.TOKEN -> ManageAssetIconModel.Token(assetIconProvider.getAssetIconOrFallback(url))
            ManageAssetGroup.Kind.NETWORK -> ManageAssetIconModel.Network(url)
        }
    }

    /**
     * What a group of this kind contains: token groups hold networks and the other way round.
     */
    private fun ManageAssetGroup.Kind.inner(): ManageAssetGroup.Kind {
        return when (this) {
            ManageAssetGroup.Kind.TOKEN -> ManageAssetGroup.Kind.NETWORK
            ManageAssetGroup.Kind.NETWORK -> ManageAssetGroup.Kind.TOKEN
        }
    }

    private fun ManageAssetGroup.Kind.allLabel(): Int {
        return when (this) {
            ManageAssetGroup.Kind.NETWORK -> R.string.assets_manage_tokens_all_networks
            ManageAssetGroup.Kind.TOKEN -> R.string.assets_manage_tokens_all_tokens
        }
    }
}
