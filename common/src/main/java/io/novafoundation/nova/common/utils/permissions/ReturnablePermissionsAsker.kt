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

    fun createReturnable(
        fragment: Fragment,
        router: ReturnableRouter
    ): Presentation = ReturnablePermissionsAsker(actionAwaitableMixinFactory, fragment, router)

    fun create(
        fragment: Fragment
    ): Presentation = BasePermissionsAsker(actionAwaitableMixinFactory, fragment)
}

private open class BasePermissionsAsker(
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    protected val fragment: Fragment
) : Presentation {

    override val showPermissionsDenied = actionAwaitableMixinFactory.create<PermissionDeniedLevel, PermissionDeniedAction>()

    override suspend fun requirePermissions(vararg permissions: Permission): Boolean {
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
                PermissionDeniedAction.RETRY -> return onRetry(e, *permissions)

                PermissionDeniedAction.CANCEL -> return onCancel()
            }
        }
    }

    protected suspend fun onRetry(e: PermissionException, vararg permissions: Permission): Boolean {
        if (e.hasDenied()) {
            return requirePermissions(*permissions)
        }

        if (e.hasForeverDenied()) {
            e.goToSettings()

            return false
        }

        return true
    }

    protected open suspend fun onCancel(): Boolean {
        return false
    }
}

private class ReturnablePermissionsAsker(
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    fragment: Fragment,
    private val returnableRouter: ReturnableRouter
) : BasePermissionsAsker(actionAwaitableMixinFactory, fragment) {

    override suspend fun onCancel(): Boolean {
        returnableRouter.back()

        return false
    }
}
