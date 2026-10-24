package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDashboardSnapshot
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDashboardSnapshotRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

/** Lifetime is bound to the selected account by the dashboard interactor. */
@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDashboardDetailsSource(
    private val positions: BittensorPositionRepository,
    private val validators: BittensorValidatorInteractor,
    private val recommendedYields: BittensorRecommendedYieldInteractor,
    private val snapshots: BittensorDashboardSnapshotRepository,
    private val logger: Logger
) {

    private data class SubnetRates(
        val values: Map<BittensorNetuid.Subnet, Map<AccountIdKey, BigDecimal>>,
        val loading: Boolean
    )

    // Unknown positions never trigger the recommendations request: PENDING waits, UNAVAILABLE settles without it.
    private enum class StakeActivity { ACTIVE, NONE, PENDING, UNAVAILABLE }

    fun observe(account: MetaAccount, chains: Collection<Chain>, maximumYieldRefreshes: SharedFlow<Unit>): Flow<Map<String, BittensorDashboardDetails>> {
        val chain = chains.firstOrNull { chain ->
            chain.assets.any { Chain.Asset.StakingType.BITTENSOR_ROOT in it.staking }
        } ?: return flowOf(emptyMap())
        val coldKey = account.accountIdIn(chain) ?: return observe(chain, null, maximumYieldRefreshes)

        return flow { emitAll(observe(chain, coldKey, maximumYieldRefreshes)) }
    }

    private fun observe(chain: Chain, coldKey: AccountId?, maximumYieldRefreshes: SharedFlow<Unit>): Flow<Map<String, BittensorDashboardDetails>> {
        val portfolio = if (coldKey != null) {
            positions.observePortfolio(chain.id, coldKey)
        } else {
            flowOf(BittensorPortfolioState(BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap()), BittensorDataStatus.Fresh))
        }
        // Like iOS persisted dashboard details: shown until positions are known again (cold start, offline).
        val restored = coldKey?.let { snapshots.snapshot(chain.id, it) }
        var lastSaved = restored
        // Like iOS MaxApyUpdate: only a successful request replaces the stored maximum yield.
        var latestMaximumYield = restored?.maximumAnnualPercentageYield
        val claimable = coldKey?.let { snapshots.claimableRootRewardsFlow(chain.id, it) } ?: flowOf(null)
        // As iOS: yields are requested once per held set, never polled.
        val rootRates = portfolio
            .map { it.positions?.root.orEmpty().map { position -> position.validatorHotkey }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest { hotkeys ->
                if (hotkeys.isEmpty()) flowOf(ExtendedLoadingState.Loaded(emptyMap())) else requested { validators.yields(BittensorNetuid.Root) }
            }
        val subnetRates = combine(portfolio, claimable) { state, rewards -> subnetsShowingRate(state.positions, rewards) }
            .distinctUntilChanged()
            .flatMapLatest(::observeSubnetRates)
        val maximumRates = combine(portfolio, claimable) { state, rewards -> stakeActivity(state, rewards, restored) }
            .distinctUntilChanged()
            .flatMapLatest { activity ->
                // As iOS: the start-staking yield is only needed, and requested, once there is known to be no active stake.
                when (activity) {
                    StakeActivity.ACTIVE, StakeActivity.UNAVAILABLE -> flowOf(null)
                    StakeActivity.PENDING -> flowOf(ExtendedLoadingState.Loading)
                    StakeActivity.NONE -> maximumYieldRequests(maximumYieldRefreshes)
                }
            }

        return combine(portfolio, rootRates, subnetRates, maximumRates, claimable) { state, root, subnets, maximum, rewards ->
            if (maximum is ExtendedLoadingState.Loaded) latestMaximumYield = maximumYield(maximum.data)

            val snapshot = restored.takeIf { state.positions == null }
            val details = if (snapshot != null) {
                snapshot.toDetails(rateLoading = state.status is BittensorDataStatus.Loading)
                    .copy(claimableRootRewardsInPlanks = rewards)
            } else {
                mapBittensorDashboardDetails(
                    positions = state.positions,
                    rootYields = root.dataOrNull.orEmpty(),
                    subnetYields = subnets.values,
                    recommendedYields = emptyMap(),
                    pricingPending = state.positions == null || state.pricesPending,
                    claimableRootRewards = rewards
                ).copy(
                    rootYieldLoading = root is ExtendedLoadingState.Loading,
                    subnetYieldLoading = subnets.loading
                )
            }.copy(
                maximumAnnualPercentageYield = latestMaximumYield,
                maximumYieldLoading = maximum is ExtendedLoadingState.Loading && latestMaximumYield == null
            )

            if (coldKey != null && state.status is BittensorDataStatus.Fresh && details.pricing != BittensorDashboardDetails.Pricing.LOADING) {
                val toSave = details.toSnapshot(lastSaved)
                if (toSave != lastSaved) {
                    snapshots.saveSnapshot(chain.id, coldKey, toSave)
                    lastSaved = toSave
                }
            }

            mapOf(chain.id to details)
        }
    }

    private fun stakeActivity(
        state: BittensorPortfolioState,
        claimableRewards: BigInteger?,
        restored: BittensorDashboardSnapshot?
    ): StakeActivity {
        if (claimableRewards?.isPositive() == true) return StakeActivity.ACTIVE
        val positions = state.positions
        return when {
            positions != null && (positions.root.isNotEmpty() || positions.subnets.isNotEmpty()) -> StakeActivity.ACTIVE
            positions != null -> StakeActivity.NONE
            restored != null && (restored.rootStakeInPlanks != null || restored.subnetCount > 0) -> StakeActivity.ACTIVE
            state.status is BittensorDataStatus.Loading -> StakeActivity.PENDING
            else -> StakeActivity.UNAVAILABLE
        }
    }

    private fun subnetsShowingRate(positions: BittensorPortfolioPositions?, claimableRewards: BigInteger?): Set<BittensorNetuid.Subnet> {
        // As iOS: the subnet yield is only shown, and requested, without root stake or claimable root rewards.
        if (positions == null || positions.root.isNotEmpty() || claimableRewards?.isPositive() == true) return emptySet()
        return positions.subnets.map { it.subnetId }.toSet()
    }

    private fun BittensorDashboardSnapshot.toDetails(rateLoading: Boolean) = BittensorDashboardDetails(
        totalStakeInPlanks = totalStakeInPlanks,
        rootStakeInPlanks = rootStakeInPlanks,
        subnetCount = subnetCount,
        rootAnnualPercentageYield = rootAnnualPercentageYield,
        subnetAnnualPercentageYield = subnetAnnualPercentageYield,
        maximumAnnualPercentageYield = maximumAnnualPercentageYield,
        pricing = if (totalStakeInPlanks != null) BittensorDashboardDetails.Pricing.KNOWN else BittensorDashboardDetails.Pricing.UNKNOWN,
        rootYieldLoading = rateLoading && rootAnnualPercentageYield == null,
        subnetYieldLoading = rateLoading && subnetAnnualPercentageYield == null
    )

    // A rate that is still loading or failed keeps the last known one while that part is still held.
    private fun BittensorDashboardDetails.toSnapshot(previous: BittensorDashboardSnapshot?) = BittensorDashboardSnapshot(
        totalStakeInPlanks = totalStakeInPlanks,
        rootStakeInPlanks = rootStakeInPlanks,
        subnetCount = subnetCount,
        rootAnnualPercentageYield = rootAnnualPercentageYield
            ?: previous?.rootAnnualPercentageYield?.takeIf { rootStakeInPlanks != null && previous.rootStakeInPlanks != null },
        subnetAnnualPercentageYield = subnetAnnualPercentageYield
            ?: previous?.subnetAnnualPercentageYield?.takeIf { subnetCount > 0 && previous.subnetCount > 0 },
        maximumAnnualPercentageYield = maximumAnnualPercentageYield
    )

    private fun observeSubnetRates(
        subnets: Set<BittensorNetuid.Subnet>
    ): Flow<SubnetRates> {
        if (subnets.isEmpty()) return flowOf(SubnetRates(emptyMap(), false))
        val sources = subnets.map { subnet -> requested { validators.yields(subnet) }.map { subnet to it } }
        return combine(sources) { results ->
            SubnetRates(
                values = results.associate { it.first to it.second.dataOrNull.orEmpty() },
                loading = results.any { it.second is ExtendedLoadingState.Loading }
            )
        }
    }

    // Refreshes are only heard while there is known to be no active stake; iOS refreshMaxApy also skips a wallet with active staking.
    // As iOS performMaxApyFetch: a refresh while a request is in flight does not start another one.
    private fun maximumYieldRequests(refreshes: SharedFlow<Unit>): Flow<ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>> = channelFlow {
        var request: Job? = null
        refreshes.onSubscription { emit(Unit) }.collect {
            if (request?.isActive != true) {
                request = launch { requested { recommendedYields.maximumYields() }.collect { send(it) } }
            }
        }
    }

    // An unavailable rate never suppresses positions or other rates.
    private fun <T> requested(load: suspend () -> T): Flow<ExtendedLoadingState<T>> = flow {
        emit(ExtendedLoadingState.Loading)
        val result = try {
            ExtendedLoadingState.Loaded(load())
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            logFailure(timeout)
            ExtendedLoadingState.Error(timeout)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logFailure(failure)
            ExtendedLoadingState.Error(failure)
        }
        emit(result)
    }

    private fun logFailure(failure: Throwable) {
        logger.log("[BittensorDashboard] Yield request failed")
        logger.log(failure)
    }
}
