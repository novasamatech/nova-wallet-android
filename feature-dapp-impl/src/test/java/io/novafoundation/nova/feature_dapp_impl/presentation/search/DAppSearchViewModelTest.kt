package io.novafoundation.nova.feature_dapp_impl.presentation.search

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsFlushReason
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingKind
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchGroup
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResultDetails
import io.novafoundation.nova.feature_dapp_impl.domain.search.SearchDappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.argThat
import io.novafoundation.nova.test_shared.eq
import io.novafoundation.nova.test_shared.whenever
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class DAppSearchViewModelTest {

    private val router = mock(DAppRouter::class.java)

    private val resourceManager = mock(ResourceManager::class.java)

    private val resultDetails = mutableMapOf<DappSearchResult, DappSearchResultDetails>()

    private val interactor = mock(SearchDappInteractor::class.java)

    private val dappInteractor = mock(DappInteractor::class.java)

    private val dAppSearchResponder = mock(DAppSearchResponder::class.java)

    private val awaitableFactory = CommonModule().actionAwaitableMixinFactory()

    private val appLinksProvider = mock(AppLinksProvider::class.java)

    private val analyticsService = RecordingAnalyticsService()

    private lateinit var viewModel: DAppSearchViewModel

    @Before
    fun setup() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        ArchTaskExecutor.getInstance().setDelegate(ImmediateTaskExecutor())
        whenever(interactor.categories()).thenReturn(emptyFlow<List<DappCategory>>())
        whenever(interactor.searchDapps(any(), any())).thenReturn(emptyMap<DappSearchGroup, List<DappSearchResult>>())
        whenever(interactor.resolveSearchResult(any())).thenAnswer { invocation -> resultDetails.getValue(invocation.getArgument(0)) }
        whenever(appLinksProvider.email).thenReturn("support@novawallet.io")

        viewModel = createViewModel(SearchPayload.Request.OPEN_NEW_URL)
    }

    @After
    fun tearDown() = runBlocking {
        viewModel.coroutineContext.job.cancelAndJoin()
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

        verify(router).openBittensorStaking(StakingFlowSource.DAPP_SEARCH_BANNER)
        assertEvents(bannerShown("bittensor"), bannerAction("bittensor"))
    }

    @Test
    fun `banner of a staking query opens the staking tab as before`() {
        typeQueries("polkadot staking")

        viewModel.stakingBannerClicked()

        verify(router).openStaking()
        assertEvents(bannerShown("staking"), bannerAction("staking"))
    }

    @Test
    fun `TAO related address shows the Bittensor notice and does not open the site`() {
        val site = DappSearchResult.Url("https://taoyield.com", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingKind.BITTENSOR)

        viewModel.searchResultClicked(site)

        val notice = stakingNotice()
        assertEquals(DAppStakingKind.BITTENSOR, notice?.payload)
        verifyNoInteractions(router)
        assertNull(unknownDAppWarning())
        assertEvents(noticeShown("bittensor", "taoyield.com"))
    }

    @Test
    fun `continuing from the Bittensor notice opens an unknown site without the unknown DApp warning`() {
        val site = DappSearchResult.Url("https://taoyield.com", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingKind.BITTENSOR)
        viewModel.searchResultClicked(site)

        selectNoticeAction(StakingNoticeAction.CONTINUE_TO_SITE)

        assertNull(unknownDAppWarning())
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
        givenSiteKind(site, DAppStakingKind.BITTENSOR)
        viewModel.searchResultClicked(site)

        selectNoticeAction(StakingNoticeAction.GO_TO_STAKING)

        verify(router).openBittensorStaking(StakingFlowSource.DAPP_NOTICE)
        assertEvents(
            noticeShown("bittensor", "www.tao.app"),
            noticeAction("bittensor", "go_to_staking", "www.tao.app")
        )
    }

    @Test
    fun `third party staking site keeps the generic notice and the staking tab`() {
        val site = DappSearchResult.Url("https://staking.polkadot.cloud", isTrustedByNova = true)
        givenSiteKind(site, DAppStakingKind.STAKING)

        viewModel.searchResultClicked(site)
        val notice = stakingNotice()
        selectNoticeAction(StakingNoticeAction.GO_TO_STAKING)

        assertEquals(DAppStakingKind.STAKING, notice?.payload)
        verify(router).openStaking()
        assertEvents(
            noticeShown("staking", "staking.polkadot.cloud"),
            noticeAction("staking", "go_to_staking", "staking.polkadot.cloud")
        )
    }

    @Test
    fun `continuing from the generic notice to an unknown site still awaits the unknown DApp warning`() {
        val site = DappSearchResult.Url("https://staking.example.org", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingKind.STAKING)
        viewModel.searchResultClicked(site)

        selectNoticeAction(StakingNoticeAction.CONTINUE_TO_SITE)

        assertEquals("support@novawallet.io", unknownDAppWarning()?.payload?.supportEmail)
        verifyNoInteractions(router)

        requireNotNull(unknownDAppWarning()).onSuccess(Unit)

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
        givenSiteKind(site, null)

        viewModel.searchResultClicked(site)

        assertNull(stakingNotice())
        assertEquals("support@novawallet.io", unknownDAppWarning()?.payload?.supportEmail)
        verifyNoInteractions(router)

        requireNotNull(unknownDAppWarning()).onSuccess(Unit)

        assertOpenedInBrowser("https://example.org")
        assertEvents(dAppOpened("example.org", source = "manual_url", isKnownDApp = false))
    }

    @Test
    fun `free text TAO query shows no notice and keeps the unknown DApp warning`() {
        val search = DappSearchResult.Search("tao", "https://duckduckgo.com/?q=tao")
        givenSiteKind(search, null)
        typeQueries("tao")

        viewModel.searchResultClicked(search)

        assertNull(stakingNotice())
        assertEquals("support@novawallet.io", unknownDAppWarning()?.payload?.supportEmail)
        verifyNoInteractions(router)
        assertEvents(bannerShown("bittensor"))
    }

    @Test
    fun `another tap cannot replace an open notice or its destination`() {
        val firstSite = DappSearchResult.Url("https://taostats.io", isTrustedByNova = true)
        val secondSite = DappSearchResult.Url("https://staking.polkadot.cloud", isTrustedByNova = true)
        givenSiteKind(firstSite, DAppStakingKind.BITTENSOR)
        givenSiteKind(secondSite, DAppStakingKind.STAKING)

        viewModel.searchResultClicked(firstSite)
        viewModel.searchResultClicked(secondSite)
        selectNoticeAction(StakingNoticeAction.CONTINUE_TO_SITE)

        assertOpenedInBrowser(firstSite.targetUrl)
        assertEvents(
            noticeShown("bittensor", "taostats.io"),
            noticeAction("bittensor", "continue_to_site", "taostats.io"),
            dAppOpened("taostats.io", source = "manual_url", isKnownDApp = true)
        )
    }

    @Test
    fun `dismissing a notice lets the user choose another result`() {
        val firstSite = DappSearchResult.Url("https://taostats.io", isTrustedByNova = true)
        val secondSite = DappSearchResult.Url("https://staking.polkadot.cloud", isTrustedByNova = true)
        givenSiteKind(firstSite, DAppStakingKind.BITTENSOR)
        givenSiteKind(secondSite, DAppStakingKind.STAKING)

        viewModel.searchResultClicked(firstSite)
        requireNotNull(stakingNotice()).onCancel()

        viewModel.searchResultClicked(secondSite)
        selectNoticeAction(StakingNoticeAction.GO_TO_STAKING)

        verify(router).openStaking()
        verifyNoMoreInteractions(router)
        assertEvents(
            noticeShown("bittensor", "taostats.io"),
            noticeShown("staking", "staking.polkadot.cloud"),
            noticeAction("staking", "go_to_staking", "staking.polkadot.cloud")
        )
    }

    @Test
    fun `an action from a cancelled notice cannot open its site`() {
        val site = DappSearchResult.Url("https://taostats.io", isTrustedByNova = true)
        givenSiteKind(site, DAppStakingKind.BITTENSOR)

        viewModel.searchResultClicked(site)
        val previousNotice = requireNotNull(stakingNotice())
        viewModel.cancelSearchResultOpening()
        previousNotice.onSuccess(StakingNoticeAction.CONTINUE_TO_SITE)

        verifyNoInteractions(router)
        assertEvents(noticeShown("bittensor", "taostats.io"))
    }

    @Test
    fun `continuing from the Bittensor notice returns the address to the browser that asked for it`() = runBlocking<Unit> {
        viewModel.coroutineContext.job.cancelAndJoin()
        viewModel = createViewModel(SearchPayload.Request.GO_TO_URL)
        val site = DappSearchResult.Url("https://taostats.io", isTrustedByNova = false)
        givenSiteKind(site, DAppStakingKind.BITTENSOR)
        viewModel.searchResultClicked(site)

        selectNoticeAction(StakingNoticeAction.CONTINUE_TO_SITE)

        verify(dAppSearchResponder).respond(
            argThat { response -> response is DAppSearchCommunicator.Response.NewUrl && response.url == site.targetUrl }
        )
        verify(router).finishDappSearch()
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

    private fun givenSiteKind(result: DappSearchResult, kind: DAppStakingKind?) {
        val host = Urls.hostOf(result.targetUrl)
        resultDetails[result] = DappSearchResultDetails(host, kind)
    }

    private fun stakingNotice() = viewModel.stakingNotice.awaitableActionLiveData.value?.peekContent()

    private fun unknownDAppWarning() = viewModel.dAppNotInCatalogWarning.awaitableActionLiveData.value?.peekContent()

    private fun selectNoticeAction(action: StakingNoticeAction) {
        requireNotNull(stakingNotice()).onSuccess(action)
    }

    private fun assertOpenedInBrowser(url: String) {
        verify(router).openDAppBrowser(
            payload = argThat { payload -> payload is DAppBrowserPayload.Address && payload.address == url },
            extras = eq(null)
        )
        verifyNoMoreInteractions(router)
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

    private class ImmediateTaskExecutor : TaskExecutor() {

        override fun executeOnDiskIO(runnable: Runnable) = runnable.run()

        override fun postToMainThread(runnable: Runnable) = runnable.run()

        override fun isMainThread() = true
    }
}
