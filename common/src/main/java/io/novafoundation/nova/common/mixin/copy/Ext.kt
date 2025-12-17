package io.novafoundation.nova.common.mixin.copy

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ResourceManager

suspend fun CopyTextLauncher.Presentation.showCopyCallHash(
    resourceManager: ResourceManager,
    value: String
) {
    showCopyTextDialog(
        CopyTextLauncher.Payload(
            title = value,
            textToCopy = value,
            resourceManager.getString(R.string.common_copy_hash),
            resourceManager.getString(R.string.common_share_hash)
        )
    )
}
