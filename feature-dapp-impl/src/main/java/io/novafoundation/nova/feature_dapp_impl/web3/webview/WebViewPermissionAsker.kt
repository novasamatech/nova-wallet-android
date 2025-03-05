package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.Manifest
import android.webkit.PermissionRequest
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WebViewPermissionAsker(
    private val permissionsAsker: PermissionsAsker.Presentation
) {

    fun requestPermission(coroutineScope: CoroutineScope, request: PermissionRequest) {
        coroutineScope.launch {
            val permissions = mapPermissionRequest(request)

            if (permissions.isNotEmpty()) {
                val result = permissionsAsker.requirePermissionsOrExit(*permissions)

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
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            else -> emptyList()
        }
    }.toTypedArray()
}
