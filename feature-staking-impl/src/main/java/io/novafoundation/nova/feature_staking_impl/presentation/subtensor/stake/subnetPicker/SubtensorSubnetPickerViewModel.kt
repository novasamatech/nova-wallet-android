package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorSubnetInfo
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class SubtensorSubnetPickerViewModel(
    private val router: StakingRouter,
    private val subnetFetcher: SubtensorSubnetFetcher,
) : BaseViewModel() {

    sealed interface UiState {
        object Loading : UiState
        object Empty : UiState
        data class Loaded(val rows: List<SubnetRow>) : UiState
    }

    data class SubnetRow(
        val netuid: Int,
        val displayName: String,
        val netuidLabel: String,
        val taoReserveText: String,
        val priceText: String,
    )

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadedSubnets: List<SubtensorSubnetInfo> = emptyList()

    init {
        launch { load() }
    }

    private suspend fun load() {
        _state.value = UiState.Loading
        val subnets = runCatching {
            withContext(Dispatchers.IO) { subnetFetcher.fetchAllSubnets() }
        }.getOrDefault(emptyList())
        loadedSubnets = subnets
        if (subnets.isEmpty()) {
            _state.value = UiState.Empty
            return
        }
        // Match iOS default sort: total stake descending. Lets users see
        // the largest subnets at the top — same UX as the validator picker.
        val rows = subnets
            .sortedByDescending { it.taoReserve }
            .map(::toRow)
        _state.value = UiState.Loaded(rows)
    }

    fun subnetClicked(netuid: Int) {
        val name = loadedSubnets.firstOrNull { it.netuid == netuid }?.name
        router.openSubtensorStakeSetup(netuid = netuid, subnetName = name)
    }

    fun backClicked() {
        router.back()
    }

    private fun toRow(info: SubtensorSubnetInfo): SubnetRow {
        return SubnetRow(
            netuid = info.netuid,
            displayName = info.name?.takeIf { it.isNotBlank() } ?: "Subnet ${info.netuid}",
            netuidLabel = "SN${info.netuid}",
            taoReserveText = formatTaoWhole(info.taoReserve),
            priceText = formatSpotPrice(info.spotPrice),
        )
    }

    private fun formatTaoWhole(rao: BigInteger): String {
        if (rao.signum() <= 0) return "—"
        // 1 TAO = 1e9 RAO. The picker shows whole TAO so big-pool subnets
        // read at a glance — the spot price column carries the precision.
        val taoWhole = rao.divide(BigInteger.TEN.pow(9))
        return "%,d TAO".format(taoWhole.toLong())
    }

    private fun formatSpotPrice(spotPrice: Double): String {
        if (spotPrice <= 0.0) return "—"
        return "%.4f TAO/α".format(spotPrice)
    }
}
