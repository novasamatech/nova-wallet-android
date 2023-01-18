package io.novafoundation.nova.feature_update_notification_impl.data

import android.content.Context
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_update_notification_impl.domain.UpdateNotification


class VersionService(
    val context: Context,
    val preferences: Preferences,
    val versionsFetcher: VersionsFetcher
) {

    private val currentVersion =
        private var versions: List<VersionResponse> = listOf()

    suspend fun hasNewVersions(): Boolean {
        return syncAndGetVersions()
    }

    suspend fun getVersions(): List<UpdateNotification> {

    }

    suspend fun hedVersionsForCurrentVersion() {

    }

    private suspend fun syncAndGetVersions(): List<VersionResponse> {
        if (versions.isEmpty()) {
            versions = versionsFetcher.getVersions()
        }

        return versions
    }

    @Suppress("DEPRECATION")
    private fun getVersion(): Long {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName.versionToLong()
    }

    // 1.9.24 = 1 / (9 / 24) = 2.666
    // 1.10.1 = 1 / 10 = 0.1
    //
    private fun String.versionToLong(): Long {
        val cleanedVersion = this.replace("[^\\d.]", "")
        val separatedVersion = cleanedVersion.split(".")
        val major = separatedVersion[0].toLong() * 10000
        val minor = separatedVersion[1].toLong() * 100
        val patch = separatedVersion[2].toLong()
        return major + minor + patch
    }
}
