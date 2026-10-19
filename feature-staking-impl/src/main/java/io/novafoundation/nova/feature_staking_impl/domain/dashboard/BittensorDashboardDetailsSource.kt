package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/** Lifetime is bound to the selected account by the dashboard interactor. */
@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDashboardDetailsSource(
    private val positions: BittensorPositionRepository,
    private val validators: BittensorValidatorInteractor,
    private val recommendedYields: BittensorRecommendedYieldInteractor,
    private val logger: Logger
) {

    private data class SubnetRates(
        val values: Map<BittensorNetuid.Subnet, Map<AccountIdKey, BigDecimal>>,
        val loading: Boolean
    )

    private companion object {
        const val REFRESH_MILLIS = 60_000L
    }

    fun observe(account: MetaAccount, chains: Collection<Chain>): Flow<Map<String, BittensorDashboardDetails>> {
        val chain = chains.firstOrNull { chain ->
            chain.assets.any { Chain.Asset.StakingType.BITTENSOR_ROOT in it.staking }
        } ?: return flowOf(emptyMap())
        val coldKey = account.accountIdIn(chain)
        val portfolio = if (coldKey != null) {
            positions.observePortfolio(chain.id, coldKey)
        } else {
            flowOf(BittensorPortfolioState(BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap()), BittensorDataStatus.Fresh))
        }
        val rootRates = refreshed { validators.yields(BittensorNetuid.Root) }
        val subnetRates = portfolio
            .map { it.positions?.subnets.orEmpty().map { position -> position.subnetId }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest(::observeSubnetRates)
        val maximumRates = refreshed { recommendedYields.maximumYields() }

        return combine(portfolio, rootRates, subnetRates, maximumRates) { state, root, subnets, maximum ->
            val details = mapBittensorDashboardDetails(
                positions = state.positions,
                rootYields = root.dataOrNull.orEmpty(),
                subnetYields = subnets.values,
                recommendedYields = maximum.dataOrNull.orEmpty()
            ).copy(
                rootYieldLoading = root is ExtendedLoadingState.Loading,
                subnetYieldLoading = subnets.loading,
                maximumYieldLoading = maximum is ExtendedLoadingState.Loading
            )
            mapOf(chain.id to details)
        }
    }

    private fun observeSubnetRates(
        subnets: Set<BittensorNetuid.Subnet>
    ): Flow<SubnetRates> {
        if (subnets.isEmpty()) return flowOf(SubnetRates(emptyMap(), false))
        val sources = subnets.map { subnet -> refreshed { validators.yields(subnet) }.map { subnet to it } }
        return combine(sources) { results ->
            SubnetRates(
                values = results.associate { it.first to it.second.dataOrNull.orEmpty() },
                loading = results.any { it.second is ExtendedLoadingState.Loading }
            )
        }
    }

    // Independent refresh/recovery: an unavailable rate never suppresses positions or other rates.
    private fun <T> refreshed(load: suspend () -> T): Flow<ExtendedLoadingState<T>> = flow {
        emit(ExtendedLoadingState.Loading)
        while (true) {
            try {
                emit(ExtendedLoadingState.Loaded(load()))
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                logFailure(timeout)
                emit(ExtendedLoadingState.Error(timeout))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logFailure(failure)
                emit(ExtendedLoadingState.Error(failure))
            }
            delay(REFRESH_MILLIS)
        }
    }

    private fun logFailure(failure: Throwable) {
        logger.log("[BittensorDashboard] Yield refresh failed")
        logger.log(failure)
    }
}
