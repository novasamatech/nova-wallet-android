package io.novafoundation.nova.feature_versions_impl.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

interface AppVersionProvider {

    fun getCurrentVersionName(): String
}

class PackageManagerAppVersionProvider(
    private val context: Context
) : AppVersionProvider {

    override fun getCurrentVersionName(): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName!!
    }
}
