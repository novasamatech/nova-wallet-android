package io.novafoundation.nova.common.data.announcements

import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.coroutines.DangerousScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.test_shared.whenever
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Locale

private const val AWAIT_TIMEOUT = 1_000L

@OptIn(DangerousScope::class, ExperimentalCoroutinesApi::class)
class AnnouncementsRepositoryTest {

    private val contextManager = mock(ContextManager::class.java)

    private lateinit var rootScope: RootScope

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        rootScope = RootScope()

        whenever(contextManager.getLocale()).thenReturn(Locale.ENGLISH)
    }

    @After
    fun tearDown() {
        rootScope.cancel()

        Dispatchers.resetMain()
    }

    @Test
    fun `announcements are available before the request completes`() = runBlocking {
        val repository = repository(NeverRespondingApi())

        val first = withTimeout(AWAIT_TIMEOUT) {
            repository.announcementsFlow(AnnouncementSection.STAKING).first()
        }

        assertEquals(emptyList<Any>(), first)
    }

    @Test
    fun `loaded announcements replace the initial empty value`() = runBlocking {
        val repository = repository(StaticApi(mapOf("staking" to listOf(REMOTE_ANNOUNCEMENT))))

        val loaded = withTimeout(AWAIT_TIMEOUT) {
            repository.announcementsFlow(AnnouncementSection.STAKING).first { it.isNotEmpty() }
        }

        assertEquals(listOf("Rewards will resume"), loaded.map { it.description })
    }

    private fun repository(api: AnnouncementsApi): AnnouncementsRepository {
        return RealAnnouncementsRepository(api, contextManager, rootScope)
    }
}

private val REMOTE_ANNOUNCEMENT = AnnouncementRemote(
    chainId = null,
    style = "warning",
    description = mapOf("default" to "Rewards will resume")
)

/**
 * Stands in for any request that never produces a response - an offline device, a blocked host, a stalled connection
 */
private class NeverRespondingApi : AnnouncementsApi {

    override suspend fun getAnnouncements(): AnnouncementsRemote = CompletableDeferred<AnnouncementsRemote>().await()
}

private class StaticApi(private val response: AnnouncementsRemote) : AnnouncementsApi {

    override suspend fun getAnnouncements(): AnnouncementsRemote = response
}
