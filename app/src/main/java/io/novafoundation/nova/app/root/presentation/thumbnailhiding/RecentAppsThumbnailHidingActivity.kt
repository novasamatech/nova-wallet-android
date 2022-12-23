package io.novafoundation.nova.app.root.presentation.thumbnailhiding

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.novafoundation.nova.app.root.presentation.thumbnailhiding.navbar.NavigationBarObserver.isGestureEnabled

abstract class RecentAppsThumbnailHidingActivity :
    AppCompatActivity(),
    RecentAppsThumbnailHidingListener {

    /**
     * HardwareKeyWatcher doesn't work on API 25 or lower,
     * allow to use FLAG_SECURE instead to hide app thumbnail.
     */
    open val enableSecureFlagOnLowApiDevices: Boolean = false

    /**
     * HardwareKeyWatcher doesn't work on some Xiaomi or Huawei devices with custom gesture navigation enabled,
     * allow to use FLAG_SECURE instead to hide app thumbnail.
     */
    open val enableSecureFlagOnCustomGestureNavigationDevices: Boolean = false

    private val isSecureFlagOnLowApiDevicesEnabled: Boolean
        get() = enableSecureFlagOnLowApiDevices && Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    private val isSecureFlagOnCustomGestureNavigationDevicesEnabled: Boolean
        get() = enableSecureFlagOnCustomGestureNavigationDevices && isGestureEnabled(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isSecureFlagOnLowApiDevicesEnabled || isSecureFlagOnCustomGestureNavigationDevicesEnabled) {
            enableSecureFlag(true)
        }
    }
}

fun Activity.enableSecureFlag(isEnabled: Boolean) {
    if (isEnabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}

fun Activity.isSecureFlagEnabled(): Boolean {
    val flags = window.attributes.flags
    return (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
}
