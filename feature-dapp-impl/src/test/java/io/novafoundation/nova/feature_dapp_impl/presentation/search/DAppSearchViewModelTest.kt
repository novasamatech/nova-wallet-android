package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.net.Uri
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsFlushReason
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingSiteKind
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchGroup
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_impl.domain.search.SearchDappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`

/**
 * The view model runs on an unconfined main dispatcher and the interactor is a mock that answers at once,
 * so every step happens on the test thread; the static [Uri] mock is only visible there.
 *
 * The interactor mock is configured only at creation: the search results flow calls it from a background thread,
 * and stubbing a mock while another thread calls it is not safe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DAppSearchViewModelTest {

    private val router = mock(DAppRouter::class.java)

    private val resourceManager = mock(ResourceManager::class.java)

    private val siteKinds = ConcurrentHashMap<DappSearchResult, DAppStakingSiteKind>()

    private val interactor = mock(SearchDappInteractor::class.java) { invocation ->
        when (invocation.method.name) {
            "categories" -> emptyFlow<List<DappCategory>>()
            "searchDapps" -> emptyMap<DappSearchGroup, List<DappSearchResult>>()
            "stakingSiteKind" -> siteKinds.getValue(invocation.getArgument(0))
            else -> RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val dappInteractor = mock(DappInteractor::class.java)

    private val dAppSearchResponder = mock(DAppSearchResponder::class.java)

    private val awaitableFactory = HeldAwaitableFactory()

    private val appLinksProvider = mock(AppLinksProvider::class.java)

    private val analyticsService = RecordingAnalyticsService()

    private lateinit var uriMock: MockedStatic<Uri>

    private lateinit var viewModel: DAppSearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        ArchTaskExecutor.getInstance().setDelegate(ImmediateTaskExecutor())
        uriMock = mockStatic(Uri::class.java)
        uriMock.`when`<Uri> { Uri.parse(anyString()) }.thenAnswer { invocation -> uriOf(invocation.getArgument(0)) }

        `when`(appLinksProvider.email).thenReturn("support@novawallet.io")

        viewModel = createViewModel(SearchPayload.Request.OPEN_NEW_URL)
    }

    @After
    fun tearDown() = runBlocking {
        viewModel.coroutineContext.job.cancelAndJoin()
        uriMock.close()
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    @Test
    fun `banner is visible for TAO related and staking queries only`() = runBlocking {
        viewModel.query.value = "tao"
        assertTrue(viewModel.stakingBannerVisible.first())

        viewModel.query.value = "hydration"
        assertFalse(viewModel.stakingBannerVisible.first())

        viewModel.query.value = "staking"
        assertTrue(viewModel.stakingBannerVisible.first())

        viewModel.query.value = ""
        assertFalse(viewModel.stakingBannerVisible.first())
    }

    @Test
    fun `banner is reported once per appearance and not per typed character`() {
        typeQueries("t", "ta", "tao", "taos")

        assertEvents(bannerShown("bittensor"))

        typeQueries("", "t", "ta", "tao")

        assertEvents(bannerShown("bittensor"), bannerShown("bittensor"))
    }

    @Test
    fun `banner is reported again when its kind changes`() {
        typeQueries("staking", "staking tao", "staking")

        assertEvents(bannerShown("staking"), bannerShown("bittensor"), bannerShown("staking"))
    }

    @Test
    fun `hidden banner reports nothing`() {
        typeQueries("h", "hydration", "taobao", "")

        assertEvents()
    }

    @Test
    fun `banner of a query matching both rules opens Bittensor staking`() {
        typeQueries("TAO staking")

        viewModel.stakingBannerClicked()

        assertRouterCalls("openBittensorStaking" to listOf(StakingFlowSource.DAPP_SEARCH_BANNER))
        assertEvents(bannerShown("bittensor"), bannerAction("bittensor"))
    }

    @Test
    fun `banner of a staking query opens the staking tab as before`() {
        typeQueries("polkadot staking")

        viewModel.stakingBannerClicked()

        assertRouterCalls("openStaking" to emptyList())
        assertEvents(bannerShown("staking"), bannerAction("staking"))
    }

    @Test
    fun `TAO related address shows the Bittensor notice and does not open the site`() {
        val site = DappSearchResult.Url("https://taoyield.com", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingSiteKind.BITTENSOR)

        viewModel.searchResultClicked(site)

        val notice = shownNotice()
        assertEquals(DAppStakingSiteKind.BITTENSOR, notice?.kind)
        assertEquals("taoyield.com", notice?.host)
        assertRouterCalls()
        assertTrue(awaitableFactory.unknownDAppWarning.requests.isEmpty())
        assertEvents(noticeShown("bittensor", "taoyield.com"))
    }

    @Test
    fun `continuing from the Bittensor notice opens an unknown site without the unknown DApp warning`() {
        val site = DappSearchResult.Url("https://taoyield.com", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingSiteKind.BITTENSOR)
        viewModel.searchResultClicked(site)

        viewModel.continueToStakingSiteClicked()

        assertTrue(awaitableFactory.unknownDAppWarning.requests.isEmpty())
        assertOpenedInBrowser("https://taoyield.com")
        assertEvents(
            noticeShown("bittensor", "taoyield.com"),
            noticeAction("bittensor", "continue_to_site", "taoyield.com"),
            dAppOpened("taoyield.com", source = "manual_url", isKnownDApp = false)
        )
    }

    @Test
    fun `go to staking from the Bittensor notice opens Bittensor staking and not the site`() {
        val site = DappSearchResult.Dapp(dApp("https://www.tao.app/staking"))
        givenSiteKind(site, DAppStakingSiteKind.BITTENSOR)
        viewModel.searchResultClicked(site)

        viewModel.noticeGoToStakingClicked()

        assertRouterCalls("openBittensorStaking" to listOf(StakingFlowSource.DAPP_NOTICE))
        assertEvents(
            noticeShown("bittensor", "www.tao.app"),
            noticeAction("bittensor", "go_to_staking", "www.tao.app")
        )
    }

    @Test
    fun `third party staking site keeps the generic notice and the staking tab`() {
        val site = DappSearchResult.Url("https://staking.polkadot.cloud", isTrustedByNova = true)
        givenSiteKind(site, DAppStakingSiteKind.THIRD_PARTY_STAKING)

        viewModel.searchResultClicked(site)
        val notice = shownNotice()
        viewModel.noticeGoToStakingClicked()

        assertEquals(DAppStakingSiteKind.THIRD_PARTY_STAKING, notice?.kind)
        assertEquals("staking.polkadot.cloud", notice?.host)
        assertRouterCalls("openStaking" to emptyList())
        assertEvents(
            noticeShown("staking", "staking.polkadot.cloud"),
            noticeAction("staking", "go_to_staking", "staking.polkadot.cloud")
        )
    }

    @Test
    fun `continuing from the generic notice to an unknown site still awaits the unknown DApp warning`() {
        val site = DappSearchResult.Url("https://staking.example.org", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingSiteKind.THIRD_PARTY_STAKING)
        viewModel.searchResultClicked(site)

        viewModel.continueToStakingSiteClicked()

        assertEquals(1, awaitableFactory.unknownDAppWarning.requests.size)
        assertRouterCalls()

        awaitableFactory.unknownDAppWarning.confirm()

        assertOpenedInBrowser("https://staking.example.org")
        assertEvents(
            noticeShown("staking", "staking.example.org"),
            noticeAction("staking", "continue_to_site", "staking.example.org"),
            dAppOpened("staking.example.org", source = "manual_url", isKnownDApp = false)
        )
    }

    @Test
    fun `unknown site without a notice still awaits the unknown DApp warning`() {
        val site = DappSearchResult.Url("https://example.org", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingSiteKind.NONE)

        viewModel.searchResultClicked(site)

        assertNull(shownNotice())
        assertEquals(1, awaitableFactory.unknownDAppWarning.requests.size)
        assertRouterCalls()

        awaitableFactory.unknownDAppWarning.confirm()

        assertOpenedInBrowser("https://example.org")
        assertEvents(dAppOpened("example.org", source = "manual_url", isKnownDApp = false))
    }

    @Test
    fun `free text TAO query shows no notice and keeps the unknown DApp warning`() {
        val search = DappSearchResult.Search("tao", "https://duckduckgo.com/?q=tao")
        givenSiteKind(search, DAppStakingSiteKind.NONE)
        typeQueries("tao")

        viewModel.searchResultClicked(search)

        assertNull(shownNotice())
        assertEquals(1, awaitableFactory.unknownDAppWarning.requests.size)
        assertRouterCalls()
        assertEvents(bannerShown("bittensor"))
    }

    @Test
    fun `notice buttons do nothing once the pending site is consumed`() {
        val site = DappSearchResult.Url("https://taostats.io", isTrustedByNova = true)
        givenSiteKind(site, DAppStakingSiteKind.BITTENSOR)
        viewModel.searchResultClicked(site)
        viewModel.noticeGoToStakingClicked()

        viewModel.continueToStakingSiteClicked()
        viewModel.noticeGoToStakingClicked()

        assertRouterCalls("openBittensorStaking" to listOf(StakingFlowSource.DAPP_NOTICE))
    }

    @Test
    fun `continuing from the Bittensor notice returns the address to the browser that asked for it`() = runBlocking<Unit> {
        viewModel.coroutineContext.job.cancelAndJoin()
        viewModel = createViewModel(SearchPayload.Request.GO_TO_URL)
        val site = DappSearchResult.Url("https://taostats.io", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingSiteKind.BITTENSOR)
        viewModel.searchResultClicked(site)

        viewModel.continueToStakingSiteClicked()

        val response = mockingDetails(dAppSearchResponder).invocations.single().arguments.single()
        assertEquals("https://taostats.io", (response as DAppSearchCommunicator.Response.NewUrl).url)
        assertRouterCalls("finishDappSearch" to emptyList())
    }

    private fun createViewModel(request: SearchPayload.Request) = DAppSearchViewModel(
        router = router,
        resourceManager = resourceManager,
        interactor = interactor,
        dappInteractor = dappInteractor,
        payload = SearchPayload(initialUrl = null, request = request),
        dAppSearchResponder = dAppSearchResponder,
        actionAwaitableMixinFactory = awaitableFactory,
        appLinksProvider = appLinksProvider,
        analyticsService = analyticsService
    )

    private fun typeQueries(vararg queries: String) {
        queries.forEach { viewModel.query.value = it }
    }

    private fun givenSiteKind(result: DappSearchResult, kind: DAppStakingSiteKind) {
        siteKinds[result] = kind
    }

    private fun shownNotice(): StakingNoticeModel? = viewModel.showStakingNoticeEvent.value?.peekContent()

    private fun assertRouterCalls(vararg expected: Pair<String, List<Any?>>) {
        val actual = mockingDetails(router).invocations.map { it.method.name to it.arguments.toList() }

        assertEquals(expected.toList(), actual)
    }

    private fun assertOpenedInBrowser(url: String) {
        val invocation = mockingDetails(router).invocations.single()

        assertEquals("openDAppBrowser", invocation.method.name)
        assertEquals(url, (invocation.arguments.first() as DAppBrowserPayload.Address).address)
    }

    private fun assertEvents(vararg expected: Pair<String, Map<String, Any>>) {
        val actual = analyticsService.events.map { it.name to it.properties }

        assertEquals(expected.toList(), actual)
    }

    private fun bannerShown(kind: String) = "dapp_staking_prompt_shown" to mapOf<String, Any>(
        "placement" to "search_banner",
        "kind" to kind
    )

    private fun bannerAction(kind: String) = "dapp_staking_prompt_action" to mapOf<String, Any>(
        "placement" to "search_banner",
        "kind" to kind,
        "action" to "go_to_staking"
    )

    private fun noticeShown(kind: String, host: String) = "dapp_staking_prompt_shown" to mapOf<String, Any>(
        "placement" to "notice",
        "kind" to kind,
        "dapp_host" to host
    )

    private fun noticeAction(kind: String, action: String, host: String) = "dapp_staking_prompt_action" to mapOf<String, Any>(
        "placement" to "notice",
        "kind" to kind,
        "action" to action,
        "dapp_host" to host
    )

    private fun dAppOpened(host: String, source: String, isKnownDApp: Boolean) = "dapp_opened" to mapOf<String, Any>(
        "dapp_host" to host,
        "source" to source,
        "is_known_dapp" to isKnownDApp
    )

    private fun uriOf(url: String): Uri {
        val uri = mock(Uri::class.java)
        `when`(uri.host).thenReturn(URI(url).host)

        return uri
    }

    private fun dApp(url: String) = DApp(
        name = url,
        description = "",
        iconLink = null,
        url = url,
        isFavourite = false,
        favoriteIndex = null
    )

    private class RecordingAnalyticsService : AnalyticsService {

        override var isEnabled = true

        val events = mutableListOf<AnalyticsEvent>()

        override fun track(event: AnalyticsEvent) {
            events += event
        }

        override suspend fun flush(reason: AnalyticsFlushReason) = Unit
    }

    /**
     * The search view model creates a single awaitable: the unknown DApp warning.
     */
    private class HeldAwaitableFactory : ActionAwaitableMixin.Factory {

        val unknownDAppWarning = HeldConfirmation<DappUnknownWarningModel>()

        @Suppress("UNCHECKED_CAST")
        override fun <P, R> create(): ActionAwaitableMixin.Presentation<P, R> {
            return unknownDAppWarning as ActionAwaitableMixin.Presentation<P, R>
        }
    }

    private class HeldConfirmation<P> : ActionAwaitableMixin.Presentation<P, Unit> {

        override val awaitableActionLiveData = MutableLiveData<Event<ActionAwaitableMixin.Action<P, Unit>>>()

        val requests = mutableListOf<P>()

        private val confirmation = CompletableDeferred<Unit>()

        override suspend fun awaitAction(payload: P) {
            requests += payload

            confirmation.await()
        }

        fun confirm() {
            confirmation.complete(Unit)
        }
    }

    private class ImmediateTaskExecutor : TaskExecutor() {

        override fun executeOnDiskIO(runnable: Runnable) = runnable.run()

        override fun postToMainThread(runnable: Runnable) = runnable.run()

        override fun isMainThread() = true
    }
}
