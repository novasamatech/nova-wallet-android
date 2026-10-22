package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatterProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.announcements.StakingAnnouncementsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.RecordingBittensorAnalytics
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapperFactory
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkDestination
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.BittensorPortfolioDeepLink
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTarget
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class StakingDashboardDeepLinkTest {

    private val interactor = mock(StakingDashboardInteractor::class.java)
    private val accountUseCase = mock(SelectedAccountUseCase::class.java)
    private val resourceManager = mock(ResourceManager::class.java)
    private val updateSystem = mock(StakingDashboardUpdateSystem::class.java)
    private val dashboardRouter = mock(StakingDashboardRouter::class.java)
    private val router = mock(StakingRouter::class.java)
    private val startRouter = mock(StartMultiStakingRouter::class.java)
    private val sharedState = StakingSharedState()
    private val mapperFactory = mock(StakingDashboardPresentationMapperFactory::class.java)
    private val formatterProvider = mock(MaskableValueFormatterProvider::class.java)
    private val amountFormatter = mock(AmountFormatter::class.java)
    private val iconProvider = mock(AssetIconProvider::class.java)
    private val announcements = mock(StakingAnnouncementsUseCase::class.java)
    private val analytics = RecordingBittensorAnalytics()
    private val chain = mock(Chain::class.java)
    private val asset = mock(Chain.Asset::class.java)
    private val token = mock(Token::class.java)
    private val target = StakingDeepLinkTarget("chain", 0)
    private val dashboardState = MutableStateFlow<ExtendedLoadingState<StakingDashboard>>(ExtendedLoadingState.Loading)
    private val noStake = NoStake(
        ExtendedLoadingState.Loading,
        NoStake.FlowType.Single(Chain.Asset.StakingType.BITTENSOR_ROOT, false),
        BigInteger.ZERO
    )
    private lateinit var viewModel: StakingDashboardViewModel

    @Before
    fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(chain.id).thenReturn("chain")
        `when`(chain.name).thenReturn("Bittensor")
        `when`(asset.id).thenReturn(0)
        `when`(token.configuration).thenReturn(asset)
        `when`(formatterProvider.provideFormatter()).thenReturn(emptyFlow())
        `when`(accountUseCase.selectedWalletModelFlow()).thenReturn(emptyFlow())
        `when`(announcements.generalAnnouncementsFlow()).thenReturn(emptyFlow())
        `when`(announcements.announcementsByChainFlow()).thenReturn(emptyFlow())
        `when`(updateSystem.start()).thenReturn(emptyFlow())
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCED)
        val dashboard = StakingDashboard(emptyList(), listOf(option))
        dashboardState.value = ExtendedLoadingState.Loaded(dashboard)
        `when`(interactor.stakingDashboardFlow()).thenReturn(dashboardState)

        viewModel = StakingDashboardViewModel(
            interactor = interactor,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            stakingDashboardUpdateSystem = updateSystem,
            dashboardRouter = dashboardRouter,
            router = router,
            startMultiStakingRouter = startRouter,
            stakingSharedState = sharedState,
            presentationMapperFactory = mapperFactory,
            maskableValueFormatterProvider = formatterProvider,
            amountFormatter = amountFormatter,
            assetIconProvider = iconProvider,
            stakingAnnouncementsUseCase = announcements,
            analyticsService = analytics
        )
    }

    @After
    fun tearDown() = runBlocking {
        if (::viewModel.isInitialized) viewModel.coroutineContext.job.cancelAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `Bittensor link opens portfolio while dashboard is still loading and is consumed once`() = runBlocking {
        dashboardState.value = ExtendedLoadingState.Loading
        val immediateTarget = target.copy(destination = StakingDeepLinkTarget.Destination.BITTENSOR_PORTFOLIO)
        viewModel.setDeepLinkTarget(immediateTarget)

        val destination = withTimeout(5_000) { viewModel.deepLinkDestination.filterNotNull().first() }
        assertTrue(destination is BittensorPortfolioDeepLink)
        viewModel.openDeepLinkDestination(destination)
        viewModel.openDeepLinkDestination(destination)

        verify(router).openBittensorPortfolio(
            entry = io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioEntry.DEEP_LINK
        )
        verifyNoInteractions(startRouter)
    }

    @Test
    fun `cancelled immediate Bittensor link does not reopen portfolio`() {
        val immediateTarget = target.copy(destination = StakingDeepLinkTarget.Destination.BITTENSOR_PORTFOLIO)
        val destination = BittensorPortfolioDeepLink(immediateTarget)
        viewModel.setDeepLinkTarget(immediateTarget)
        viewModel.setDeepLinkTarget(null)

        viewModel.openDeepLinkDestination(destination)

        verifyNoInteractions(router, startRouter)
    }

    @Test
    fun `no stake deep link opens introductory landing only once with deep link analytics`() {
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCED)
        val destination = StakingDeepLinkDestination(target, option)
        viewModel.setDeepLinkTarget(target)

        viewModel.openDeepLinkDestination(destination)
        viewModel.openDeepLinkDestination(destination)

        assertLanding("deep_link")
        verifyNoInteractions(router)
    }

    @Test
    fun `existing Bittensor stake deep link opens portfolio instead of introductory landing`() {
        val state = HasStake(false, Chain.Asset.StakingType.BITTENSOR_ROOT, null, ExtendedLoadingState.Loading)
        val option = AggregatedStakingDashboardOption(chain, token, state, SyncingStage.SYNCED)
        val destination = StakingDeepLinkDestination(target, option)
        viewModel.setDeepLinkTarget(target)

        viewModel.openDeepLinkDestination(destination)
        viewModel.openDeepLinkDestination(destination)

        verify(router).openBittensorPortfolio()
        verifyNoInteractions(startRouter)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `ordinary dashboard tap keeps dashboard analytics`() = runBlocking {
        withTimeout(5_000) { viewModel.onNoStakeItemClicked(0).join() }

        assertLanding("dashboard")
        verifyNoInteractions(router)
    }

    @Test
    fun `cancelled deep link does not navigate`() {
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCED)
        val destination = StakingDeepLinkDestination(target, option)
        viewModel.setDeepLinkTarget(target)
        viewModel.setDeepLinkTarget(null)

        viewModel.openDeepLinkDestination(destination)

        verifyNoInteractions(router, startRouter)
        assertTrue(analytics.events.isEmpty())
    }

    private fun assertLanding(source: String) {
        val invocation = mockingDetails(startRouter).invocations.single()
        assertEquals("openStartStakingLanding", invocation.method.name)
        val payload = invocation.arguments.single() as StartStakingLandingPayload
        assertEquals("chain", payload.availableStakingOptions.chainId)
        assertEquals(0, payload.availableStakingOptions.assetId)
        assertEquals(listOf(Chain.Asset.StakingType.BITTENSOR_ROOT), payload.availableStakingOptions.stakingTypes)
        val event = analytics.events.single()
        assertEquals("staking_flow_opened", event.name)
        assertEquals(mapOf("network" to "Bittensor", "source" to source), event.properties)
    }
}
