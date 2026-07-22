package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker

import android.graphics.drawable.Drawable
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorValidator
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubtensorValidatorPickerViewModel(
    private val router: StakingRouter,
    private val validatorProvider: SubtensorValidatorProvider,
    private val stakingSharedState: StakingSharedState,
    @Suppress("UnusedPrivateMember") private val chainRegistry: ChainRegistry,
    private val addressIconGenerator: AddressIconGenerator,
    val netuid: Int,
) : BaseViewModel() {

    enum class Sort { APR_DESC, TOTAL_STAKE_DESC }

    /**
     * Sort + show filters sheet state. Mirrors iOS
     * `SubtensorValidatorFilterViewController.State` exactly:
     * default = APR-desc with no toggles on.
     */
    data class FilterState(
        val requireIdentity: Boolean = false,
        val hideMaxCommission: Boolean = false,
        val sort: Sort = Sort.APR_DESC,
    ) {
        val isDefault: Boolean
            get() = !requireIdentity && !hideMaxCommission && sort == Sort.APR_DESC

        companion object {
            val DEFAULT = FilterState()
        }
    }

    sealed interface UiState {
        object Loading : UiState
        object Empty : UiState
        object Error : UiState
        data class Loaded(val rows: List<ValidatorRow>) : UiState
    }

    data class ValidatorRow(
        val hotkey: String,
        val identity: String?,
        val displayName: String,
        val isHotkeyDisplayed: Boolean,
        val identicon: Drawable?,
        val aprText: String,
        val commissionText: String,
    )

    private val _allRows = MutableStateFlow<List<ValidatorRow>?>(null)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState.DEFAULT)
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.Loading)

    /**
     * Final UI state — combines load status, all rows, search query, and
     * filters into the currently-visible list. iOS does the equivalent
     * inside `applyValidators(validators)` on every filter / search change.
     */
    val state: StateFlow<UiState> = combine(
        _loadStatus,
        _allRows,
        _searchQuery,
        _filterState,
    ) { status, rows, query, filters -> resolve(status, rows, query, filters) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private var loadedValidators: List<SubtensorValidator> = emptyList()
    private var addressPrefix: Short = 42

    init {
        launch { load() }
    }

    private suspend fun load() {
        _loadStatus.value = LoadStatus.Loading
        addressPrefix = stakingSharedState.chain().addressPrefix.toShort()
        val validators = runCatching {
            withContext(Dispatchers.IO) { validatorProvider.fetchValidators(netuid) }
        }.getOrElse {
            _loadStatus.value = LoadStatus.Error
            return
        }
        loadedValidators = validators
        _allRows.value = validators.map { toRow(it) }
        _loadStatus.value = LoadStatus.Loaded
    }

    fun rowClicked(hotkey: String) {
        val identity = loadedValidators.firstOrNull {
            runCatching { it.hotkey.toAddress(addressPrefix) }.getOrNull() == hotkey
        }?.identity?.takeIf { it.isNotBlank() }
        router.respondAndPopValidatorPicker(SubtensorPickedValidator(hotkey, identity))
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
        allRows: List<ValidatorRow>?,
        query: String,
        filters: FilterState,
    ): UiState = when (status) {
        LoadStatus.Loading -> UiState.Loading
        LoadStatus.Error -> UiState.Error
        LoadStatus.Loaded -> {
            val rows = allRows ?: emptyList()
            val filtered = applyFilters(rows, filters)
            val searched = applySearch(filtered, query)
            val sorted = applySort(searched, filters.sort)
            if (sorted.isEmpty()) UiState.Empty else UiState.Loaded(sorted)
        }
    }

    private fun applyFilters(rows: List<ValidatorRow>, filters: FilterState): List<ValidatorRow> {
        if (filters.isDefault) return rows
        val byIdentity = if (filters.requireIdentity) {
            rows.filter { !it.isHotkeyDisplayed }
        } else {
            rows
        }
        val byCommission = if (filters.hideMaxCommission) {
            // iOS hides commission >= 1.0 (i.e. 100%). We mirror by inspecting
            // the raw validator since the row only carries formatted text.
            byIdentity.filter { row ->
                val commission = loadedValidators.firstOrNull { it.hotkey.toAddressOrNull() == row.hotkey }?.commission
                commission == null || commission < 1.0
            }
        } else {
            byIdentity
        }
        return byCommission
    }

    private fun applySearch(rows: List<ValidatorRow>, query: String): List<ValidatorRow> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return rows
        val lower = trimmed.lowercase()
        return rows.filter { row ->
            val identityHit = row.identity?.lowercase()?.contains(lower) == true
            val hotkeyHit = row.hotkey.lowercase().contains(lower)
            identityHit || hotkeyHit
        }
    }

    private fun applySort(rows: List<ValidatorRow>, sort: Sort): List<ValidatorRow> {
        return when (sort) {
            Sort.APR_DESC -> rows.sortedWith(
                compareByDescending<ValidatorRow> { rowApr(it) ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { rowTotalStake(it) }
            )
            Sort.TOTAL_STAKE_DESC -> rows.sortedByDescending { rowTotalStake(it) }
        }
    }

    private fun rowApr(row: ValidatorRow): Double? =
        loadedValidators.firstOrNull { it.hotkey.toAddressOrNull() == row.hotkey }?.apr

    private fun rowTotalStake(row: ValidatorRow): java.math.BigInteger =
        loadedValidators.firstOrNull { it.hotkey.toAddressOrNull() == row.hotkey }?.totalStake
            ?: java.math.BigInteger.ZERO

    private fun ByteArray.toAddressOrNull(): String? =
        runCatching { this.toAddress(addressPrefix) }.getOrNull()

    private suspend fun toRow(validator: SubtensorValidator): ValidatorRow {
        val displayAddress = runCatching { validator.hotkey.toAddress(addressPrefix) }.getOrDefault("")
        val identity = validator.identity?.takeIf { it.isNotBlank() }
        val isHotkey = identity == null
        val displayName = identity ?: shorten(displayAddress)
        val identicon = runCatching {
            withContext(Dispatchers.Default) {
                addressIconGenerator.createAddressIcon(validator.hotkey, ICON_SIZE_DP)
            }
        }.getOrNull()
        return ValidatorRow(
            hotkey = displayAddress,
            identity = identity,
            displayName = displayName,
            isHotkeyDisplayed = isHotkey,
            identicon = identicon,
            aprText = formatApr(validator.apr),
            commissionText = formatCommission(validator.commission),
        )
    }

    private fun shorten(address: String): String =
        if (address.length <= 10) address else "${address.take(6)}...${address.takeLast(4)}"

    private fun formatApr(apr: Double?): String {
        if (apr == null) return "—"
        return "%.2f%%".format(apr * 100.0)
    }

    private fun formatCommission(commission: Double?): String {
        if (commission == null) return ""
        return "%d%% commission".format((commission * 100.0).toInt())
    }

    private enum class LoadStatus { Loading, Loaded, Error }

    companion object {
        private const val ICON_SIZE_DP = 24
    }
}
