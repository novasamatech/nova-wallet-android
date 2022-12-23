package io.novafoundation.nova.app.root.presentation.thumbnailhiding

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.novafoundation.nova.app.root.presentation.thumbnailhiding.navbar.NavigationBarObserver
import io.novafoundation.nova.app.root.presentation.thumbnailhiding.navbar.OnNavigationBarListener

class RecentAppsThumbnailHidingLifecycleTracker : Application.ActivityLifecycleCallbacks {

    private var hardwareKeyWatcher: HardwareKeyWatcher? = null

    // Detect custom gesture navigation enabled on Xiaomi or Huawei devices to switch to use FLAG_SECURE
    private var navigationBarObserver: NavigationBarObserver? = null
    private var navigationBarListener: OnNavigationBarListener? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (NavigationBarObserver.isAvailable()) {
            if (navigationBarObserver?.activityContext != activity) {
                disposeNavigationBarObserver()
            }
            navigationBarListener = OnNavigationBarListener { isGestureEnabled ->
                if (activity is RecentAppsThumbnailHidingActivity &&
                    activity.enableSecureFlagOnCustomGestureNavigationDevices
                ) {
                    activity.enableSecureFlag(isGestureEnabled)
                }
            }
            navigationBarObserver = NavigationBarObserver.getInstance().apply {
                register(activity)
                addOnNavigationBarListener(navigationBarListener)
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (hardwareKeyWatcher?.context != activity) {
            disposeHardwareKeyWatcher()
        }
        hardwareKeyWatcher = HardwareKeyWatcher(activity).apply {
            setOnHardwareKeysPressedListenerListener(object :
                    HardwareKeyWatcher.OnHardwareKeysPressedListener {
                    override fun onHomePressed() {
                        activity.triggerRecentAppsMode(true)
                    }

                    override fun onRecentAppsPressed() {
                        activity.triggerRecentAppsMode(true)
                    }
                })
            startWatch()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        activity.triggerRecentAppsMode(false)
    }

    override fun onActivityPaused(activity: Activity) {
        /*
         * Fix hide app recent for 2 cases:
         * - pulldown notification > settings > recent
         * - messenger chathead > profile in fullscreen > recent
         * - Xiaomi accessibility button > recent
         */
        activity.triggerRecentAppsMode(true)
    }

    override fun onActivityStopped(activity: Activity) {
        if (hardwareKeyWatcher?.context == activity) {
            disposeHardwareKeyWatcher()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (NavigationBarObserver.isAvailable() && navigationBarObserver?.activityContext == activity) {
            disposeNavigationBarObserver()
        }
    }

    private fun disposeHardwareKeyWatcher() {
        hardwareKeyWatcher?.stopWatch()
        hardwareKeyWatcher = null
    }

    private fun disposeNavigationBarObserver() {
        navigationBarObserver?.run {
            unregister()
            removeOnNavigationBarListener(navigationBarListener)
        }
        navigationBarObserver = null
        navigationBarListener = null
    }

    /**
     * Only trigger `onRecentAppsTriggered` if the activity
     * - implements RecentAppsThumbnailHidingListener or
     * - extends RecentAppsThumbnailHidingActivity with enableSecureFlagOnLowApiDevices disabled or API 26 (or later)
     */
    private fun Activity.triggerRecentAppsMode(inRecentAppsMode: Boolean) {
        when (val activity = this) {
            is RecentAppsThumbnailHidingActivity -> if (!activity.isSecureFlagEnabled()) {
                activity.onRecentAppsTriggered(activity, inRecentAppsMode)
            }
            is RecentAppsThumbnailHidingListener ->
                activity.onRecentAppsTriggered(activity, inRecentAppsMode)
        }
    }
}
