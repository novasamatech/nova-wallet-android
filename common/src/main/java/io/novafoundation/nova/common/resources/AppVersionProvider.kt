package io.novafoundation.nova.common.resources

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

interface AppVersionProvider {

    val versionName: String
}

internal class OSAppVersionProvider(
    private val appContext: Context,
) : AppVersionProvider {

    override val versionName: String
        get() {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            return packageInfo.versionName!!
        }
}
