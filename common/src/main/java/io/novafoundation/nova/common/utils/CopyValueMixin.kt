package io.novafoundation.nova.common.utils

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager

interface CopyValueMixin {
    fun copyValue(value: String)
}

class RealCopyValueMixin(
    private val clipboardManager: ClipboardManager,
    private val toastMessageManager: ToastMessageManager,
    private val resourceManager: ResourceManager
) : CopyValueMixin {
    override fun copyValue(value: String) {
        clipboardManager.addToClipboard(value)

        toastMessageManager.showToast(resourceManager.getString(R.string.common_copied))
    }
}
