package io.novafoundation.nova.feature_versions_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class RealVersionRepositoryTest {

    @Test
    fun `should not show updates if app is updated`() = runBlocking {
        runHasImportantUpdatesTest(
            appVersion = "1.2.0",
            updates = listOf(critical("1.2.0")),
            latestSkippedVersion = null,
            isImportant = false
        )

        runHasImportantUpdatesTest(
            appVersion = "1.2.0",
            updates = listOf(normal("1.2.0")),
            latestSkippedVersion = null,
            isImportant = false
        )

        runHasImportantUpdatesTest(
            appVersion = "1.2.0",
            updates = listOf(critical("1.1.0"), major("1.2.0")),
            latestSkippedVersion = null,
            isImportant = false
        )
    }

    @Test
    fun `should not show updates if there are no updates`() = runBlocking {
        runHasImportantUpdatesTest(
            appVersion = "1.2.0",
            updates = emptyList(),
            latestSkippedVersion = null,
            isImportant = false
        )
    }

    @Test
    fun `should show updates if there is a critical update`() = runBlocking {
        runHasImportantUpdatesTest(
            appVersion = "1.0.0",
            updates = listOf(critical("1.1.0")),
            latestSkippedVersion = null,
            isImportant = true
        )

        // even if it is skipped
        runHasImportantUpdatesTest(
            appVersion = "1.0.0",
            updates = listOf(critical("1.1.0")),
            latestSkippedVersion = "1.1.0",
            isImportant = true
        )
    }

    @Test
    fun `should not show updates if there is only a normal update`() = runBlocking {
        runHasImportantUpdatesTest(
            appVersion = "1.0.0",
            updates = listOf(normal("1.0.1")),
            latestSkippedVersion = null,
            isImportant = false
        )
    }

    @Test
    fun `should not show updates if major update was skipped`() = runBlocking {
        runHasImportantUpdatesTest(
            appVersion = "1.0.0",
            updates = listOf(major("1.1.0")),
            latestSkippedVersion = "1.1.0",
            isImportant = false
        )
    }

    @Test
    fun `should not show updates if critical update is installed and others are skipped major`() = runBlocking {
        runHasImportantUpdatesTest(
            appVersion = "1.1.0",
            updates = listOf(critical("1.1.0"), major("1.2.0")),
            latestSkippedVersion = "1.2.0",
            isImportant = false
        )
    }


    private suspend fun runHasImportantUpdatesTest(
        appVersion: String,
        updates: List<Pair<String, String>>, // [(version, priority)]
        latestSkippedVersion: String?,
        isImportant: Boolean
    ) {
        val preferences = Mockito.mock(Preferences::class.java).also {
            whenever(it.getString(any())).thenReturn(latestSkippedVersion)
        }

        val versionResponses = updates.map { (version, severity) -> VersionResponse(version, severity, "2022-12-22T06:46:07Z") }
        val fetcher = Mockito.mock(VersionsFetcher::class.java).also {
            whenever(it.getVersions()).thenReturn(versionResponses)
        }

        val appVersionProvider = Mockito.mock(AppVersionProvider::class.java).also {
            whenever(it.getCurrentVersionName()).thenReturn(appVersion)
        }

        val repository = RealVersionRepository(appVersionProvider, preferences, fetcher)

        val actualIsImportant = repository.hasImportantUpdates()

        assertEquals(isImportant, actualIsImportant)
    }

    private fun critical(version: String) = version to REMOTE_SEVERITY_CRITICAL

    private fun normal(version: String) = version to REMOTE_SEVERITY_NORMAL

    private fun major(version: String) = version to REMOTE_SEVERITY_MAJOR
}
