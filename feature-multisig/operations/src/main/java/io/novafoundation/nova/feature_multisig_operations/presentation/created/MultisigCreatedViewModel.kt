package io.novafoundation.nova.feature_multisig_operations.presentation.created

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetPayload
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.fragment.ActionBottomSheetViewModel
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.common.view.bottomSheet.action.secondary
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter

class MultisigCreatedViewModel(
    private val resourceManager: ResourceManager,
    private val router: MultisigOperationsRouter
) : ActionBottomSheetViewModel(router) {

    override fun getPayload(): ActionBottomSheetPayload {
        return ActionBottomSheetPayload(
            imageRes = R.drawable.ic_multisig,
            title = resourceManager.getString(R.string.multisig_transaction_created_title),
            subtitle = getSubtitle(),
            actionButtonPreferences = ButtonPreferences.primary(primaryButtonText(), ::viewDetailsClicked),
            neutralButtonPreferences = ButtonPreferences.secondary(secondaryButtonText()),
            checkBoxPreferences = null
        )
    }

    fun viewDetailsClicked() {
        router.openPendingOperations()
    }

    private fun getSubtitle() = resourceManager.highlightedText(
        mainRes = R.string.multisig_transaction_created_subtitle,
        R.string.corrupted_backup_error_subtitle_highlighted
    )

    private fun primaryButtonText() = resourceManager.getString(R.string.multisig_transaction_created_view_details)
    private fun secondaryButtonText() = resourceManager.getString(R.string.common_close)
}
