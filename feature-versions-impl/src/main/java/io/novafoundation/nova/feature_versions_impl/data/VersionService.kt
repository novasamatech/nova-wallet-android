package io.novafoundation.nova.feature_versions_impl.data

import android.content.Context
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_versions_impl.domain.UpdateNotification
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext


class VersionService(
    private val context: Context,
    private val preferences: Preferences,
    private val versionsFetcher: VersionsFetcher
) {

    companion object {
        private const val PREF_VERSION_CHECKPOINT = "PREF_VERSION_CHECKPOINT"
    }

    private val currentVersion = getAppVersion()

    private var versions = mapOf<Version, VersionResponse>()

    suspend fun hasNewVersions(): Boolean {
        val checkpointVersion = getRecentVersionCheckpoint() ?: currentVersion
        return syncAndGetVersions()
            .any { checkpointVersion < it.key }
    }

    suspend fun getNewVersions(): List<UpdateNotification> {
        return syncAndGetVersions()
            .filter { currentVersion < it.key }
            .map { getVersionDetailsAsync(it.value) }
            .awaitAll()
    }

    fun saveVersionCheckpoint() {
        preferences.putString(PREF_VERSION_CHECKPOINT, currentVersion.toString())
    }

    private suspend fun getRecentVersionCheckpoint(): Version? {
        val checkpointVersion = preferences.getString(PREF_VERSION_CHECKPOINT)
        return checkpointVersion?.let { Version.of(it) }
    }

    private suspend fun getVersionDetailsAsync(versionResponse: VersionResponse): Deferred<UpdateNotification> {
        return withContext(Dispatchers.Default) {
            async {
                val versionFile = versionResponse.version.replace(".", "_")
                versionsFetcher.getVersionDetails(versionFile)
                mapFromRemoteVersion(versionResponse, "")
            }
        }
    }

    private suspend fun syncAndGetVersions(): Map<Version, VersionResponse> {
        if (versions.isEmpty()) {
            versions = versionsFetcher.getVersions()
                .associateBy { Version.of(it.version) }
        }

        return versions
    }

    @Suppress("DEPRECATION")
    private fun getAppVersion(): Version {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return Version.of(packageInfo.versionName)
    }

    private class Version(
        val major: Long,
        val minor: Long,
        val patch: Long
    ) {

        companion object {
            fun of(version: String): Version {
                val cleanedVersion = version.replace("[^\\d.]", "")
                val separatedVersion = cleanedVersion.split(".")
                    .map { it.toLong() }
                return Version(
                    separatedVersion[0],
                    separatedVersion[1],
                    separatedVersion[2]
                )
            }
        }

        operator fun compareTo(other: Version): Int {
            val comparedMajor = major.compareTo(other.major)
            if (comparedMajor != 0) {
                return comparedMajor
            }

            val comparedMinor = minor.compareTo(other.minor)
            if (comparedMinor != 0) {
                return comparedMinor
            }

            return patch.compareTo(other.patch)
        }

        override fun toString(): String {
            return "$major.$minor.$patch"
        }
    }
}
