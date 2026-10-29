package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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

private typealias SubnetRates = Map<BittensorNetuid.Subnet, ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>>

/** Lifetime is bound to the selected account by the dashboard interactor. */
@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDashboardDetailsSource(
    private val positions: BittensorPositionRepository,
    private val validators: BittensorValidatorInteractor,
    private val recommendedYields: BittensorRecommendedYieldInteractor,
    private val snapshots: BittensorDashboardSnapshotRepository,
    private val logger: Logger
) {

    private class DashboardInputs(
        val portfolio: BittensorPortfolioState,
        val rootRates: ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>,
        val subnetRates: SubnetRates,
        val maximumRates: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>?,
        val claimableRewards: BigInteger?
    )

    private class SubnetRateResult(
        val subnetId: BittensorNetuid.Subnet,
        val rates: ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>
    )

    // Unknown positions never trigger the recommendations request: PENDING waits, UNAVAILABLE settles without it.
    private enum class StakeActivity { ACTIVE, NONE, PENDING, UNAVAILABLE }

    // Only heard while there is known to be no active stake.
    private val maximumYieldRefreshes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Each time the dashboard appears: asks for the start-staking yield again, keeping the shown one until it succeeds.
    fun refreshMaximumYield() {
        maximumYieldRefreshes.tryEmit(Unit)
    }

    fun observe(account: MetaAccount, chains: Collection<Chain>): Flow<Map<String, BittensorDashboardDetails>> {
        val chain = chains.firstOrNull { chain ->
            chain.assets.any { Chain.Asset.StakingType.BITTENSOR_ROOT in it.staking }
        } ?: return flowOf(value = emptyMap())
        val coldKey = account.accountIdIn(chain) ?: return observe(chain, coldKey = null)

        return flow { emitAll(observe(chain, coldKey)) }
    }

    private fun observe(chain: Chain, coldKey: AccountId?): Flow<Map<String, BittensorDashboardDetails>> {
        val portfolio = observePortfolio(chain.id, coldKey)
        // Like iOS persisted dashboard details: shown until positions are known again (cold start, offline).
        val restored = coldKey?.let { snapshots.snapshot(chain.id, it) }
        var lastSaved = restored
        // Only a successful request replaces the stored maximum yield.
        var latestMaximumYield: StakingDashboardValue<BigDecimal> = restored?.maximumAnnualPercentageYield
            ?.let { StakingDashboardValue.Known(it) }
            ?: StakingDashboardValue.Unavailable
        val claimable = coldKey?.let { snapshots.claimableRootRewardsFlow(chain.id, it) } ?: flowOf(value = null)

        return combine(
            portfolio,
            observeRootRates(portfolio),
            observeSubnetRates(portfolio, claimable),
            observeMaximumRates(portfolio, claimable, restored),
            claimable,
            ::DashboardInputs
        ).map { inputs ->
            val maximumRates = inputs.maximumRates
            if (maximumRates is ExtendedLoadingState.Loaded) latestMaximumYield = maximumYield(maximumRates.data)

            val maximumYield = maximumYieldForDisplay(maximumRates, latestMaximumYield)
            val details = mapDashboardDetails(inputs, restored, maximumYield)
            lastSaved = saveFreshSnapshot(chain.id, coldKey, inputs.portfolio.status, details, lastSaved)

            mapOf(chain.id to details)
        }
    }

    private fun observePortfolio(chainId: String, coldKey: AccountId?): Flow<BittensorPortfolioState> {
        if (coldKey != null) return positions.observePortfolio(chainId, coldKey)

        val emptyPositions = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())
        return flowOf(BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh))
    }

    private fun observeRootRates(
        portfolio: Flow<BittensorPortfolioState>
    ): Flow<ExtendedLoadingState<Map<AccountIdKey, BigDecimal>>> {
        // Yields are requested once per held set, never polled.
        return portfolio
            .map { state -> state.rootValidatorHotkeys() }
            .distinctUntilChanged()
            .flatMapLatest { hotkeys ->
                if (hotkeys.isEmpty()) {
                    flowOf(ExtendedLoadingState.Loaded(data = emptyMap()))
                } else {
                    requested { validators.yields(BittensorNetuid.Root) }
                }
            }
    }

    private fun BittensorPortfolioState.rootValidatorHotkeys(): Set<AccountIdKey> {
        return positions?.root.orEmpty()
            .map { it.validatorHotkey }
            .toSet()
    }

    private fun observeSubnetRates(
        portfolio: Flow<BittensorPortfolioState>,
        claimable: Flow<BigInteger?>
    ): Flow<SubnetRates> {
        return combine(
            portfolio,
            claimable
        ) { state, rewards -> subnetsShowingRate(state.positions, rewards) }
            .distinctUntilChanged()
            .flatMapLatest(::observeSubnetRates)
    }

    private fun observeMaximumRates(
        portfolio: Flow<BittensorPortfolioState>,
        claimable: Flow<BigInteger?>,
        restored: BittensorDashboardSnapshot?
    ): Flow<ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>?> {
        return combine(
            portfolio,
            claimable
        ) { state, rewards -> stakeActivity(state, rewards, restored) }
            .distinctUntilChanged()
            .flatMapLatest { activity ->
                // As iOS: the start-staking yield is only needed, and requested, once there is known to be no active stake.
                when (activity) {
                    StakeActivity.ACTIVE, StakeActivity.UNAVAILABLE -> flowOf(value = null)
                    StakeActivity.PENDING -> flowOf(ExtendedLoadingState.Loading)
                    StakeActivity.NONE -> maximumYieldRequests()
                }
            }
    }

    private fun mapDashboardDetails(
        inputs: DashboardInputs,
        restored: BittensorDashboardSnapshot?,
        maximumYield: StakingDashboardValue<BigDecimal>
    ): BittensorDashboardDetails {
        val state = inputs.portfolio
        val snapshot = restored.takeIf { state.positions == null }
        if (snapshot != null) {
            return snapshot.toDetails(
                rateLoading = state.status is BittensorDataStatus.Loading,
                maximumYield,
                claimableRootRewards = inputs.claimableRewards
            )
        }

        return mapBittensorDashboardDetails(
            positions = state.positions,
            rootYields = inputs.rootRates,
            subnetYields = inputs.subnetRates,
            maximumAnnualPercentageYield = maximumYield,
            pricingPending = state.pricesPending || (state.positions == null && state.status is BittensorDataStatus.Loading),
            claimableRootRewards = inputs.claimableRewards
        )
    }

    private fun maximumYieldForDisplay(
        maximumRates: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>?,
        latestYield: StakingDashboardValue<BigDecimal>
    ): StakingDashboardValue<BigDecimal> {
        return if (maximumRates is ExtendedLoadingState.Loading && latestYield !is StakingDashboardValue.Known) {
            StakingDashboardValue.Loading
        } else {
            latestYield
        }
    }

    private fun saveFreshSnapshot(
        chainId: String,
        coldKey: AccountId?,
        status: BittensorDataStatus,
        details: BittensorDashboardDetails,
        previous: BittensorDashboardSnapshot?
    ): BittensorDashboardSnapshot? {
        if (coldKey == null || status !is BittensorDataStatus.Fresh || details.totalStake is StakingDashboardValue.Loading) return previous

        val snapshot = details.toSnapshot(previous)
        if (snapshot == previous) return previous

        snapshots.saveSnapshot(chainId, coldKey, snapshot)
        return snapshot
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
        // The subnet yield is only shown, and requested, without root stake or claimable root rewards.
        if (positions == null || positions.root.isNotEmpty() || claimableRewards?.isPositive() == true) return emptySet()
        return positions.subnets.map { it.subnetId }.toSet()
    }

    private fun BittensorDashboardSnapshot.toDetails(
        rateLoading: Boolean,
        maximumYield: StakingDashboardValue<BigDecimal>,
        claimableRootRewards: BigInteger?
    ) = BittensorDashboardDetails(
        totalStake = totalStakeInPlanks?.let { StakingDashboardValue.Known(it) } ?: StakingDashboardValue.Unavailable,
        rootStakeInPlanks = rootStakeInPlanks,
        subnetCount = subnetCount,
        rootAnnualPercentageYield = restoreYield(rootAnnualPercentageYield, rateLoading),
        subnetAnnualPercentageYield = restoreYield(subnetAnnualPercentageYield, rateLoading),
        maximumAnnualPercentageYield = maximumYield,
        claimableRootRewardsInPlanks = claimableRootRewards
    )

    // A rate that is still loading or failed keeps the last known one while that part is still held.
    private fun BittensorDashboardDetails.toSnapshot(previous: BittensorDashboardSnapshot?) = BittensorDashboardSnapshot(
        totalStakeInPlanks = when (val valuation = totalStake) {
            is StakingDashboardValue.Known -> valuation.value
            StakingDashboardValue.Unavailable -> null
            StakingDashboardValue.Loading -> error("Loading valuations cannot be persisted")
        },
        rootStakeInPlanks = rootStakeInPlanks,
        subnetCount = subnetCount,
        rootAnnualPercentageYield = persistedYield(
            rootAnnualPercentageYield,
            previous?.rootAnnualPercentageYield?.takeIf { rootStakeInPlanks != null && previous.rootStakeInPlanks != null }
        ),
        subnetAnnualPercentageYield = persistedYield(
            subnetAnnualPercentageYield,
            previous?.subnetAnnualPercentageYield?.takeIf { subnetCount > 0 && previous.subnetCount > 0 }
        ),
        maximumAnnualPercentageYield = persistedYield(maximumAnnualPercentageYield, previous = null)
    )

    private fun restoreYield(value: BigDecimal?, loading: Boolean): StakingDashboardValue<BigDecimal> {
        return when {
            value != null -> StakingDashboardValue.Known(value)
            loading -> StakingDashboardValue.Loading
            else -> StakingDashboardValue.Unavailable
        }
    }

    private fun persistedYield(value: StakingDashboardValue<BigDecimal>, previous: BigDecimal?): BigDecimal? {
        return when (value) {
            is StakingDashboardValue.Known -> value.value
            StakingDashboardValue.Loading, StakingDashboardValue.Unavailable -> previous
        }
    }

    private fun observeSubnetRates(
        subnets: Set<BittensorNetuid.Subnet>
    ): Flow<SubnetRates> {
        if (subnets.isEmpty()) return flowOf(value = emptyMap())
        val sources = subnets.map { subnet ->
            requested { validators.yields(subnet) }
                .map { SubnetRateResult(subnet, it) }
        }
        return combine(sources) { results ->
            results.associate { it.subnetId to it.rates }
        }
    }

    // A refresh while a request is in flight does not start another one.
    private fun maximumYieldRequests(): Flow<ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>> = channelFlow {
        var request: Job? = null
        maximumYieldRefreshes
            .onSubscription { emit(Unit) }
            .collect {
                if (request?.isActive != true) {
                    request = launch {
                        requested { recommendedYields.maximumYields() }
                            .collect { send(it) }
                    }
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
