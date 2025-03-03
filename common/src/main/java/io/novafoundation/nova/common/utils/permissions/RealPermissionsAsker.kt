package io.novafoundation.nova.common.utils.permissions

import androidx.fragment.app.Fragment
import com.github.florent37.runtimepermission.kotlin.PermissionException
import com.github.florent37.runtimepermission.kotlin.coroutines.experimental.askPermission
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker.PermissionDeniedAction
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker.PermissionDeniedLevel
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker.Presentation

class PermissionsAskerFactory(
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
) {

    fun create(
        fragment: Fragment,
        router: ReturnableRouter
    ): Presentation = RealPermissionsAsker(actionAwaitableMixinFactory, fragment, router)
}

private class RealPermissionsAsker(
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val fragment: Fragment,
    private val returnableRouter: ReturnableRouter
) : Presentation {

    override val showPermissionsDenied = actionAwaitableMixinFactory.create<PermissionDeniedLevel, PermissionDeniedAction>()

    override suspend fun requirePermissionsOrExit(vararg permissions: Permission): Boolean {
        return requirePermissionOrFallback(
            *permissions,
            requirePermission = { requirePermissionsOrExit(*it.toTypedArray()) },
            fallback = {
                returnableRouter.back()
                false
            }
        )
    }

    override suspend fun requirePermissionsOrIgnore(vararg permissions: Permission): Boolean {
        return requirePermissionOrFallback(
            *permissions,
            requirePermission = { requirePermissionsOrIgnore(*it.toTypedArray()) },
            fallback = { false }
        )
    }

    private suspend inline fun requirePermissionOrFallback(
        vararg permissions: Permission,
        requirePermission: (permissions: List<Permission>) -> Boolean,
        fallback: () -> Boolean
    ): Boolean {
        try {
            fragment.askPermission(*permissions)

            return true
        } catch (e: PermissionException) {
            val level = if (e.hasForeverDenied()) {
                PermissionDeniedLevel.REQUIRE_SETTINGS_CHANGE
            } else {
                PermissionDeniedLevel.CAN_ASK_AGAIN
            }

            when (showPermissionsDenied.awaitAction(level)) {
                PermissionDeniedAction.RETRY -> {
                    if (e.hasDenied()) {
                        return requirePermission(permissions.toList())
                    }

                    if (e.hasForeverDenied()) {
                        e.goToSettings()

                        return false
                    }

                    return true
                }

                PermissionDeniedAction.REJECT -> {
                    return fallback()
                }
            }
        }
    }
}
