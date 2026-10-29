package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetTradingAvailabilityRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.onEach

class BittensorQuickPickInteractor(
    private val subnetRepository: BittensorSubnetRepository,
    private val tradingAvailabilityRepository: BittensorSubnetTradingAvailabilityRepository,
    private val swapSource: BittensorSwapSource,
    private val selector: BittensorQuickPickSelector,
    private val recommendedYieldInteractor: BittensorRecommendedYieldInteractor
) {

    /** One visit owns its selection; retrying a read must not randomly change the subnet. */
    fun observeSelection(order: BittensorQuickPickOrder, retries: Flow<Int>): Flow<ExtendedLoadingState<BittensorSubnet>> = flow {
        var selected: BittensorSubnetReference? = null
        emitAll(
            retries.flatMapLatest {
                flowOf {
                    val catalogue = subnets()
                    val reference = selected ?: select(catalogue, order)
                    selected = reference
                    resolve(catalogue, reference)
                }.withSafeLoading().onEach { state ->
                    if (state is ExtendedLoadingState.Error && state.exception is CancellationException) throw state.exception
                }
            }
        )
    }

    suspend fun subnets(): List<BittensorSubnet> = subnetRepository.quickPickSubnets()

    suspend fun select(subnets: List<BittensorSubnet>, order: BittensorQuickPickOrder): BittensorSubnetReference {
        val annualYields = if (order == BittensorQuickPickOrder.APY) recommendedYieldInteractor.maximumYields() else emptyMap()
        return selector.select(subnets, order, annualYields)
    }

    suspend fun resolve(subnets: List<BittensorSubnet>, reference: BittensorSubnetReference): BittensorSubnet {
        val subnet = subnets.find { it.id == reference.subnetId } ?: throw SelectionInvalidated("Selected subnet is unavailable")
        if (subnet.registrationBlock != reference.registrationBlock) throw SelectionInvalidated("Selected subnet registration changed")
        require(tradingAvailabilityRepository.isEnabled(reference.subnetId)) { "Selected subnet trading is disabled" }

        val price = swapSource.currentAlphaPrice(ChainGeneses.BITTENSOR, reference.subnetId)
        require(price.isPositive()) { "Selected subnet price is unavailable" }
        return subnet.copy(priceInRao = price)
    }

    private class SelectionInvalidated(message: String) : IllegalArgumentException(message)
}
