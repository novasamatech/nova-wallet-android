package io.novafoundation.nova.feature_multisig_operations.presentation.created

import android.view.Gravity
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetPayload
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.fragment.ActionBottomSheetViewModel
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.common.view.bottomSheet.action.secondary
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter

class MultisigCreatedViewModel(
    private val resourceManager: ResourceManager,
    private val router: MultisigOperationsRouter,
    private val payload: MultisigCreatedPayload
) : ActionBottomSheetViewModel() {

    override fun getPayload(): ActionBottomSheetPayload {
        return ActionBottomSheetPayload(
            imageRes = R.drawable.ic_multisig,
            title = resourceManager.getString(R.string.multisig_transaction_created_title),
            subtitle = getSubtitle(),
            actionButtonPreferences = ButtonPreferences.primary(primaryButtonText()),
            neutralButtonPreferences = ButtonPreferences.secondary(secondaryButtonText()),
            alertModel = getAlertModel(),
            checkBoxPreferences = null
        )
    }

    private fun getAlertModel(): AlertModel? {
        return if (payload.walletWasSwitched) {
            AlertModel(
                style = AlertView.Style.fromPreset(AlertView.StylePreset.INFO, iconGravity = Gravity.CENTER),
                message = resourceManager.getString(R.string.alert_nova_has_selected_ms_wallet)
            )
        } else {
            null
        }
    }

    override fun onActionClicked() {
        router.openPendingOperations()
    }

    private fun getSubtitle() = resourceManager.highlightedText(
        mainRes = R.string.multisig_transaction_created_subtitle,
        R.string.multisig_transaction_created_subtitle_highlight
    )

    private fun primaryButtonText() = resourceManager.getString(R.string.multisig_transaction_created_view_details)
    private fun secondaryButtonText() = resourceManager.getString(R.string.common_close)
}
