package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorValidator
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.SubtensorStakeSetupFragment
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class SubtensorValidatorPickerViewModel(
    private val router: StakingRouter,
    private val validatorProvider: SubtensorValidatorProvider,
    private val stakingSharedState: StakingSharedState,
    private val chainRegistry: ChainRegistry,
    val netuid: Int,
) : BaseViewModel() {

    sealed interface UiState {
        object Loading : UiState
        object Empty : UiState
        object Error : UiState
        data class Loaded(val rows: List<ValidatorRow>) : UiState
    }

    data class ValidatorRow(
        val hotkey: String,
        val displayName: String,
        val hotkeyShort: String,
        val totalStakeText: String,
        val commissionText: String,
        val aprText: String,
    )

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadedValidators: List<SubtensorValidator> = emptyList()
    private var addressPrefix: Short = 42

    init {
        launch { load() }
    }

    private suspend fun load() {
        _state.value = UiState.Loading
        addressPrefix = stakingSharedState.chain().addressPrefix.toShort()
        val validators = runCatching {
            withContext(Dispatchers.IO) { validatorProvider.fetchValidators(netuid) }
        }.getOrElse {
            _state.value = UiState.Error
            return
        }
        loadedValidators = validators
        if (validators.isEmpty()) {
            _state.value = UiState.Empty
            return
        }
        val rows = validators.map(::toRow)
        _state.value = UiState.Loaded(rows)
    }

    fun rowClicked(hotkey: String) {
        router.respondAndPopValidatorPicker(hotkey)
    }

    fun backClicked() {
        router.back()
    }

    private fun toRow(validator: SubtensorValidator): ValidatorRow {
        val displayAddress = runCatching { validator.hotkey.toAddress(addressPrefix) }
            .getOrDefault("")
        val display = validator.identity?.takeIf { it.isNotBlank() } ?: shorten(displayAddress)
        return ValidatorRow(
            hotkey = displayAddress,
            displayName = display,
            hotkeyShort = shorten(displayAddress),
            totalStakeText = formatTao(validator.totalStake),
            commissionText = formatCommission(validator.commission),
            aprText = formatApr(validator.apr),
        )
    }

    private fun shorten(address: String): String =
        if (address.length <= 10) address else "${address.take(6)}…${address.takeLast(4)}"

    private fun formatTao(rao: BigInteger): String {
        if (rao.signum() <= 0) return "—"
        val tao = rao.toBigDecimal().movePointLeft(9)
        return "%,.2f TAO".format(tao)
    }

    private fun formatCommission(commission: Double?): String {
        if (commission == null) return ""
        return "%.1f%% fee".format(commission * 100.0)
    }

    private fun formatApr(apr: Double?): String {
        if (apr == null) return ""
        return "%.2f%% APR".format(apr * 100.0)
    }

    companion object {
        const val ARG_NETUID = "validator_picker_netuid"

        // Re-exported here for callers that don't want to import the setup
        // fragment just to write to its saved state handle.
        const val KEY_SELECTED_HOTKEY = SubtensorStakeSetupFragment.KEY_SELECTED_VALIDATOR_HOTKEY
    }
}
