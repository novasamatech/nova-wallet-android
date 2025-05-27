package io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import io.branch.referral.Branch
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_deep_linking.BuildConfig

class BranchIOLinkHandler(
    private val deepLinkFactory: BranchIoLinkConverter
) {

    object Initializer {
        fun init(context: Context) {
            if (BuildConfig.DEBUG) {
                Branch.enableLogging()
            }

            Branch.getAutoInstance(context)
        }
    }

    fun onActivityStart(activity: Activity, deepLinkCallback: (Uri) -> Unit) {
        Branch.sessionBuilder(activity)
            .withCallback { branchUniversalObject, _, error ->
                if (error != null) {
                    Log.e(LOG_TAG, error.toString())
                }

                if (branchUniversalObject != null) {
                    val deepLink = deepLinkFactory.formatToDeepLink(branchUniversalObject)
                    deepLinkCallback(deepLink)
                }
            }
            .withData(activity.intent.data)
            .init()
    }

    fun onActivityNewIntent(activity: Activity, intent: Intent?) {
        activity.intent = intent
        if (intent != null && intent.getBooleanExtra("branch_force_new_session", false)) {
            Branch.sessionBuilder(activity)
                .withCallback { _, error ->
                    if (error != null) {
                        Log.e(LOG_TAG, error.toString())
                    }
                }
                .withData(intent.data)
                .reInit()
        }
    }
}
