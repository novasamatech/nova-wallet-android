package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorValidatorFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class BittensorValidatorFiltersViewModel(
    private val request: BittensorValidatorFiltersCommunicator.Request,
    private val communicator: BittensorValidatorFiltersCommunicator,
    private val router: StakingRouter
) : BaseViewModel() {

    val tokenSymbol = request.tokenSymbol

    private val _filters = MutableStateFlow(request.filters)
    val filters = _filters.asStateFlow()

    // Reset brings back the defaults (hide unusual APY, sort by APY), so it is useless once they are set
    val isResetEnabled = filters.map { !it.isDefault }
        .shareInBackground()

    // Apply only when something changed since the screen opened, as on iOS
    val isApplyEnabled = filters.map { it != request.filters }
        .shareInBackground()

    fun hideUnusualApyChanged(hide: Boolean) {
        _filters.update { it.copy(hideUnusualApy = hide) }
    }

    fun sortingChanged(sorting: BittensorValidatorSorting) {
        _filters.update { it.copy(sorting = sorting) }
    }

    fun reset() {
        _filters.value = BittensorValidatorFilters()
    }

    fun apply() {
        val filters = _filters.value
        if (filters == request.filters) return

        communicator.respond(BittensorValidatorFiltersCommunicator.Response(filters))
        router.back()
    }

    fun back() {
        router.back()
    }
}
