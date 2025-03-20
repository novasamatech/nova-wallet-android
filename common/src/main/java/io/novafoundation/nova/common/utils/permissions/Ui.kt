package io.novafoundation.nova.common.utils.permissions

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker.PermissionDeniedAction
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker.PermissionDeniedLevel.CAN_ASK_AGAIN
import io.novafoundation.nova.common.view.dialog.warningDialog

fun BaseFragment<*>.setupPermissionAsker(
    component: PermissionsAsker,
) {
    component.showPermissionsDenied.awaitableActionLiveData.observeEvent {
        val level = it.payload

        warningDialog(
            context = requireContext(),
            onPositiveClick = { it.onSuccess(PermissionDeniedAction.RETRY) },
            onNegativeClick = { it.onSuccess(PermissionDeniedAction.CANCEL) },
            positiveTextRes = if (level == CAN_ASK_AGAIN) R.string.common_ask_again else R.string.common_to_settings
        ) {
            if (level == CAN_ASK_AGAIN) {
                setTitle(R.string.common_permission_permissions_needed_title)
                setMessage(R.string.common_permission_permissions_needed_message)
            } else {
                setTitle(R.string.common_permission_permissions_denied_title)
                setMessage(R.string.common_permission_permissions_denied_message)
            }
        }
    }
}
