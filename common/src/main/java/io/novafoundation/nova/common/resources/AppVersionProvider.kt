package io.novafoundation.nova.common.resources

import android.content.Context

interface AppVersionProvider {

    val versionName: String
}

internal class OSAppVersionProvider(
    private val appContext: Context,
) : AppVersionProvider {

    override val versionName: String
        get() = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName!!
}
