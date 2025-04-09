package io.novafoundation.nova.common.utils.browser.permissions

import android.Manifest
import android.webkit.PermissionRequest
import androidx.fragment.app.Fragment
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WebViewPermissionAskerFactory(private val permissionsAskerFactory: PermissionsAskerFactory) {

    fun create(fragment: Fragment): WebViewPermissionAsker {
        return RealWebViewPermissionAsker(permissionsAskerFactory.create(fragment))
    }
}

class RealWebViewPermissionAsker(
    private val permissionsAsker: PermissionsAsker.Presentation
) : WebViewPermissionAsker {

    override fun requestPermission(coroutineScope: CoroutineScope, request: PermissionRequest) {
        coroutineScope.launch {
            val permissions = mapPermissionRequest(request)

            if (permissions.isNotEmpty()) {
                val result = permissionsAsker.requirePermissions(*permissions)

                if (result) {
                    request.grant(request.resources)
                } else {
                    request.deny()
                }
            } else {
                request.deny()
            }
        }
    }

    private fun mapPermissionRequest(request: PermissionRequest) = request.resources.flatMap { resource ->
        when (resource) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> listOf(Manifest.permission.CAMERA)
            else -> emptyList()
        }
    }.toTypedArray()
}
