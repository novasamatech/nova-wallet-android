package io.novafoundation.nova.feature_versions_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.Version
import io.novafoundation.nova.feature_versions_api.domain.toUnderscoreString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface VersionRepository {
    suspend fun hasImportantUpdates(): Boolean

    suspend fun getNewUpdateNotifications(): List<UpdateNotification>

    suspend fun skipCurrentUpdates()

    fun inAppUpdatesCheckAllowedFlow(): Flow<Boolean>

    fun allowUpdate()

    suspend fun loadVersions()
}

class RealVersionRepository(
    private val appVersionProvider: AppVersionProvider,
    private val preferences: Preferences,
    private val versionsFetcher: VersionsFetcher
) : VersionRepository {

    companion object {
        private const val PREF_VERSION_CHECKPOINT = "PREF_VERSION_CHECKPOINT"
    }

    private val mutex = Mutex(false)

    private val appVersion = getAppVersion()

    private var versions = mapOf<Version, VersionResponse>()

    private val _inAppUpdatesCheckAllowed = MutableStateFlow(false)

    override fun allowUpdate() {
        _inAppUpdatesCheckAllowed.value = true
    }

    override suspend fun loadVersions() {
        syncAndGetVersions()
    }

    override suspend fun hasImportantUpdates(): Boolean {
        val lastSkippedVersion = getRecentVersionCheckpoint()

        return syncAndGetVersions().any { it.shouldPresentUpdate(appVersion, lastSkippedVersion) }
    }

    private fun Map.Entry<Version, VersionResponse>.shouldPresentUpdate(
        appVersion: Version,
        latestSkippedVersion: Version?,
    ): Boolean {
        val (updateVersion, updateInfo) = this

        val alreadyUpdated = appVersion >= updateVersion
        if (alreadyUpdated) return false

        val notImportantUpdate = updateInfo.severity == REMOTE_SEVERITY_NORMAL
        if (notImportantUpdate) return false

        val hasSkippedThisUpdate = latestSkippedVersion != null && latestSkippedVersion >= updateVersion
        val canBypassSkip = updateInfo.severity == REMOTE_SEVERITY_CRITICAL

        if (hasSkippedThisUpdate && !canBypassSkip) return false

        return true
    }

    override suspend fun getNewUpdateNotifications(): List<UpdateNotification> {
        return syncAndGetVersions()
            .filter { appVersion < it.key }
            .map { getChangelogAsync(it.key, it.value) }
            .awaitAll()
            .filterNotNull()
    }

    override suspend fun skipCurrentUpdates() {
        val latestUpdateNotification = syncAndGetVersions()
            .maxWith { first, second -> first.key.compareTo(second.key) }
        preferences.putString(PREF_VERSION_CHECKPOINT, latestUpdateNotification.key.toString())
    }

    override fun inAppUpdatesCheckAllowedFlow(): Flow<Boolean> {
        return _inAppUpdatesCheckAllowed
    }

    private fun getRecentVersionCheckpoint(): Version? {
        val checkpointVersion = preferences.getString(PREF_VERSION_CHECKPOINT)
        return checkpointVersion?.toVersion()
    }

    private suspend fun getChangelogAsync(version: Version, versionResponse: VersionResponse): Deferred<UpdateNotification?> {
        return coroutineScope {
            async(Dispatchers.Default) {
                val versionFileName = version.toUnderscoreString()
                val changelog = runCatching { versionsFetcher.getChangelog(versionFileName) }.getOrNull()
                mapFromRemoteVersion(version, versionResponse, changelog)
            }
        }
    }

    private suspend fun syncAndGetVersions(): Map<Version, VersionResponse> {
        return mutex.withLock {
            if (versions.isEmpty()) {
                versions = runCatching { fetchVersions() }
                    .getOrElse { emptyMap() }
            }
            versions
        }
    }

    private suspend fun fetchVersions(): Map<Version, VersionResponse> {
        return versionsFetcher.getVersions()
            .associateBy { it.version.toVersion() }
    }

    @Suppress("DEPRECATION")
    private fun getAppVersion(): Version {
        return appVersionProvider.versionName.toVersion()
    }

    private fun String.toVersion(): Version {
        val cleanedVersion = replace("[^\\d.]".toRegex(), "")
        val (major, minor, patch) = cleanedVersion.split(".")
            .map { it.toLong() }
        return Version(major, minor, patch)
    }
}
