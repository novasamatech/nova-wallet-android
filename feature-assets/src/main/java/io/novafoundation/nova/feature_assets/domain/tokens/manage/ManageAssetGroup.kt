package io.novafoundation.nova.feature_assets.domain.tokens.manage

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * One row of the manage tokens screen together with the rows it expands into.
 *
 * The screen shows the same shape in both view modes, only the axis changes: grouping by token
 * puts networks inside, grouping by network puts tokens inside. [kind] says which way round this
 * group was built, which is all the UI needs to pick the right icon treatment for each level.
 */
class ManageAssetGroup(
    val id: String,
    val title: String,
    val iconUrl: String?,
    val kind: Kind,
    /**
     * Whether the curated config still lists any of this group's assets. Drives the section the
     * group is shown in, and moves with the config rather than with what the user has enabled.
     */
    val isDefault: Boolean,
    val isEnabled: Boolean,
    val isSwitchable: Boolean,
    val items: List<ManageAssetItem>
) {

    enum class Kind {
        TOKEN, NETWORK
    }
}

class ManageAssetItem(
    val id: FullChainAssetId,
    val title: String,
    /**
     * Only set where the design shows a second line: a token inside a network group is named by
     * its ticker plus its full name, while a network inside a token group needs no subtitle.
     */
    val subtitle: String?,
    val iconUrl: String?,
    val isEnabled: Boolean,
    val isSwitchable: Boolean
)

fun ManageAssetGroup.allAssetIds(): List<FullChainAssetId> = items.map { it.id }
