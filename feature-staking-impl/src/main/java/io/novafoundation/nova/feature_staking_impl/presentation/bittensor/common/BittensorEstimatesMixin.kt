package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/** Optional estimates must not block the operation or hide its independently loaded data. */
class BittensorEstimatesMixin(
    private val costBasisInteractor: BittensorCostBasisInteractor,
    private val alphaYieldRepository: BittensorAlphaYieldRepository,
    private val logger: Logger
) {

    fun observeCostBasis(context: Flow<BittensorEstimateContext?>): Flow<ExtendedLoadingState<BittensorEstimatedCostBasis?>> =
        costBasisInteractor.observeRequests(context)
            .flatMapLatest { request ->
                flowOf { request?.let { costBasisInteractor.costBasis(it) } }
                    .withSafeLoading()
                    .onEach { state ->
                        if (state is ExtendedLoadingState.Error) reportUnavailable(state.exception)
                    }
            }

    fun observeAlphaYields(context: Flow<Chain?>, subnetId: BittensorNetuid.Subnet): Flow<Map<AccountIdKey, BigDecimal>?> =
        context.distinctUntilChanged()
            .flatMapLatest { chain ->
                flowOf { chain?.let { alphaYieldRepository.alphaYields(it, subnetId) } }
                    .onStart { emit(null) }
                    .catch { cause ->
                        reportUnavailable(cause)
                        emit(null)
                    }
            }

    private fun reportUnavailable(cause: Throwable) {
        logger.log("[BittensorEstimates] Optional estimate unavailable")
        logger.log(cause)
    }
}
