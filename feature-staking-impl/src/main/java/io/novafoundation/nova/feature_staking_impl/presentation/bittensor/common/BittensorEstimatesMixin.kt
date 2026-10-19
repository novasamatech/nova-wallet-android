package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Optional estimates must not block the operation or hide its independently loaded data. */
class BittensorEstimatesMixin(
    private val costBasisInteractor: BittensorCostBasisInteractor,
    private val alphaYieldRepository: BittensorAlphaYieldRepository,
    private val logger: Logger
) {

    fun observeCostBasis(context: Flow<BittensorEstimateContext?>): Flow<ExtendedLoadingState<BittensorEstimatedCostBasis?>> =
        costBasisInteractor.observe(context)
            .map { state ->
                when (state) {
                    BittensorCostBasisState.Unavailable -> ExtendedLoadingState.Loaded(null)
                    BittensorCostBasisState.Loading -> ExtendedLoadingState.Loading
                    is BittensorCostBasisState.Loaded -> ExtendedLoadingState.Loaded(state.basis)
                    is BittensorCostBasisState.Failed -> ExtendedLoadingState.Error(state.cause)
                }
            }
            .onEach { state ->
                if (state is ExtendedLoadingState.Error) reportUnavailable(state.exception)
            }

    fun observeAlphaYields(context: Flow<Chain?>, subnetId: BittensorNetuid.Subnet): Flow<ExtendedLoadingState<Map<AccountIdKey, BigDecimal>?>> =
        context.distinctUntilChanged()
            .flatMapLatest { chain ->
                flowOf { chain?.let { alphaYieldRepository.alphaYields(it, subnetId) } }
                    .withSafeLoading()
                    .onEach { state ->
                        if (state is ExtendedLoadingState.Error) reportUnavailable(state.exception)
                    }
            }

    private fun reportUnavailable(cause: Throwable) {
        logger.log("[BittensorEstimates] Optional estimate unavailable")
        logger.log(cause)
    }
}
