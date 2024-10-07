package io.novafoundation.nova.runtime.multiNetwork.runtime

class RuntimeCacheMigrator {

    companion object {

        private const val LATEST_VERSION = 2
    }

    fun needsMetadataFetch(localVersion: Int): Boolean {
        return localVersion < LATEST_VERSION
    }

    fun latestVersion(): Int {
        return LATEST_VERSION
    }
}
