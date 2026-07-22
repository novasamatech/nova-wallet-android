package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorSubnetInfo
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class SubtensorSubnetPickerViewModel(
    private val router: StakingRouter,
    private val subnetFetcher: SubtensorSubnetFetcher,
) : BaseViewModel() {

    /** Sort options. Mirrors iOS `SubtensorSubnetFilterViewController.Sort`. */
    enum class Sort { TOTAL_STAKE_DESC, NETUID_ASC }

    data class FilterState(val sort: Sort = Sort.TOTAL_STAKE_DESC) {
        val isDefault: Boolean
            get() = sort == Sort.TOTAL_STAKE_DESC

        companion object {
            val DEFAULT = FilterState()
        }
    }

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

    private val _allRows = MutableStateFlow<List<SubnetRow>?>(null)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState.DEFAULT)
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.Loading)

    val state: StateFlow<UiState> = combine(
        _loadStatus,
        _allRows,
        _searchQuery,
        _filterState,
    ) { status, rows, query, filters -> resolve(status, rows, query, filters) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private var loadedSubnets: List<SubtensorSubnetInfo> = emptyList()

    init {
        launch { load() }
    }

    private suspend fun load() {
        _loadStatus.value = LoadStatus.Loading
        val subnets = runCatching {
            withContext(Dispatchers.IO) { subnetFetcher.fetchAllSubnets() }
        }.getOrDefault(emptyList())
        loadedSubnets = subnets
        _allRows.value = subnets.map(::toRow)
        _loadStatus.value = LoadStatus.Loaded
    }

    fun subnetClicked(netuid: Int) {
        val name = loadedSubnets.firstOrNull { it.netuid == netuid }?.name
        router.openSubtensorStakeSetup(netuid = netuid, subnetName = name)
    }

    fun backClicked() {
        router.back()
    }

    fun searchQueryChanged(text: String) {
        _searchQuery.value = text
    }

    fun filterChanged(state: FilterState) {
        _filterState.value = state
    }

    private fun resolve(
        status: LoadStatus,
        allRows: List<SubnetRow>?,
        query: String,
        filters: FilterState,
    ): UiState = when (status) {
        LoadStatus.Loading -> UiState.Loading
        LoadStatus.Loaded -> {
            val rows = allRows ?: emptyList()
            val searched = applySearch(rows, query)
            val sorted = applySort(searched, filters.sort)
            if (sorted.isEmpty()) UiState.Empty else UiState.Loaded(sorted)
        }
    }

    private fun applySearch(rows: List<SubnetRow>, query: String): List<SubnetRow> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return rows
        val lower = trimmed.lowercase()
        return rows.filter { row ->
            row.displayName.lowercase().contains(lower) ||
                row.netuidLabel.lowercase().contains(lower) ||
                row.netuid.toString() == trimmed
        }
    }

    private fun applySort(rows: List<SubnetRow>, sort: Sort): List<SubnetRow> {
        return when (sort) {
            Sort.TOTAL_STAKE_DESC -> rows.sortedByDescending { rowReserve(it.netuid) }
            Sort.NETUID_ASC -> rows.sortedBy { it.netuid }
        }
    }

    private fun rowReserve(netuid: Int): BigInteger =
        loadedSubnets.firstOrNull { it.netuid == netuid }?.taoReserve ?: BigInteger.ZERO

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
        val taoWhole = rao.divide(BigInteger.TEN.pow(9))
        return "%,d TAO".format(taoWhole.toLong())
    }

    private fun formatSpotPrice(spotPrice: Double): String {
        if (spotPrice <= 0.0) return "—"
        return "%.4f TAO/α".format(spotPrice)
    }

    private enum class LoadStatus { Loading, Loaded }
}
