package io.novafoundation.nova.app.root.presentation.thumbnailhiding

import android.app.Activity

interface RecentAppsThumbnailHidingListener {

    /**
     * Default implementation: enable app thumbnail hiding with `FLAG_SECURE` flag
     * Override to implement custom app thumbnail hiding
     * Override with empty body or ignore interface implementation to ignore app thumbnail hiding
     */
    fun onRecentAppsTriggered(
        activity: Activity,
        inRecentAppsMode: Boolean
    ) {
        activity.enableSecureFlag(inRecentAppsMode)
    }
}
