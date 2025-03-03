package io.novafoundation.nova.common.utils.permissions

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin

typealias Permission = String

interface PermissionsAsker {
    enum class PermissionDeniedAction {
        RETRY, REJECT
    }

    enum class PermissionDeniedLevel {
        CAN_ASK_AGAIN, REQUIRE_SETTINGS_CHANGE
    }

    val showPermissionsDenied: ActionAwaitableMixin<PermissionDeniedLevel, PermissionDeniedAction>

    interface Presentation : PermissionsAsker {
        suspend fun requirePermissionsOrExit(vararg permissions: Permission): Boolean

        suspend fun requirePermissionsOrIgnore(vararg permissions: Permission): Boolean
    }
}
