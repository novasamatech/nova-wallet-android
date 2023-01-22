package io.novafoundation.nova.feature_versions_impl.data

import android.content.Context
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.Version
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

    suspend fun hasImportantUpdates(): Boolean {
        val checkpointVersion = getRecentVersionCheckpoint() ?: currentVersion
        return syncAndGetVersions()
            .any { checkpointVersion < it.key || it.value.severity == REMOTE_SEVERITY_CRITICAL }
    }

    suspend fun getNewVersions(): List<UpdateNotification> {
        return syncAndGetVersions()
            .filter { currentVersion < it.key }
            .map { getChangelogAsync(it.key, it.value) }
            .awaitAll()
    }

    fun skipCurrentUpdates() {
        preferences.putString(PREF_VERSION_CHECKPOINT, currentVersion.toString())
    }

    private fun getRecentVersionCheckpoint(): Version? {
        val checkpointVersion = preferences.getString(PREF_VERSION_CHECKPOINT)
        return checkpointVersion?.toVersion()
    }

    private suspend fun getChangelogAsync(version: Version, versionResponse: VersionResponse): Deferred<UpdateNotification> {
        return withContext(Dispatchers.Default) {
            async {
                val versionFile = versionResponse.version.replace(".", "_")
                val changelog = versionsFetcher.getChangelog(versionFile)
                mapFromRemoteVersion(version, versionResponse, changelog)
            }
        }
    }

    private suspend fun syncAndGetVersions(): Map<Version, VersionResponse> {
        if (versions.isEmpty()) {
            versions = runCatching { fetchVersions() }
                .getOrElse { emptyMap() }
        }

        return versions
    }

    private suspend fun fetchVersions(): Map<Version, VersionResponse> {
        return versionsFetcher.getVersions()
            .associateBy { it.version.toVersion() }
    }

    @Suppress("DEPRECATION")
    private fun getAppVersion(): Version {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName.toVersion()
    }


    private fun String.toVersion(): Version {
        val cleanedVersion = replace("[^\\d.]".toRegex(), "")
        val separatedVersion = cleanedVersion.split(".")
            .map { it.toLong() }
        return Version(
            separatedVersion[0],
            separatedVersion[1],
            separatedVersion[2]
        )
    }
}
