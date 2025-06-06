package io.novafoundation.nova.common.utils.permissions

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin

typealias Permission = String

interface PermissionsAsker {
    enum class PermissionDeniedAction {
        RETRY, CANCEL
    }

    enum class PermissionDeniedLevel {
        CAN_ASK_AGAIN, REQUIRE_SETTINGS_CHANGE
    }

    val showPermissionsDenied: ActionAwaitableMixin<PermissionDeniedLevel, PermissionDeniedAction>

    interface Presentation : PermissionsAsker {
        suspend fun requirePermissions(vararg permissions: Permission): Boolean

        fun checkPermissions(vararg permissions: Permission): Boolean
    }
}

fun PermissionsAsker.Presentation.checkPermissions(permissions: List<Permission>): Boolean {
    return checkPermissions(*permissions.toTypedArray())
}
