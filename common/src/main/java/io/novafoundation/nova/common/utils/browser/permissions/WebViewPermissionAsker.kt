package io.novafoundation.nova.common.utils.browser.permissions

import android.webkit.PermissionRequest
import kotlinx.coroutines.CoroutineScope

interface WebViewPermissionAsker {

    fun requestPermission(coroutineScope: CoroutineScope, request: PermissionRequest)
}
