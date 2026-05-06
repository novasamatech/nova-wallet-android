package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.SubtensorStakeSubmitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.runtime.state.chainAsset
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SubtensorStakeSetupViewModel(
    private val router: StakingRouter,
    private val dashboardRouter: StakingDashboardRouter,
    private val resourceManager: ResourceManager,
    private val stakingSharedState: StakingSharedState,
    private val submitInteractor: SubtensorStakeSubmitInteractor,
    val netuid: Int,
    private val subnetName: String?,
) : BaseViewModel() {

    val isRoot: Boolean = netuid == SubtensorStakingConstants.ROOT_NETUID

    val titleText: StateFlow<String> = MutableStateFlow(resolveTitle()).asStateFlow()

    private val _selectedValidatorHotkey = MutableStateFlow<String?>(null)
    val selectedValidatorHotkey: StateFlow<String?> = _selectedValidatorHotkey.asStateFlow()

    val selectedValidatorLabel: StateFlow<String> = _selectedValidatorHotkey
        .map(::formatValidatorLabel)
        .stateIn(viewModelScope, SharingStarted.Eagerly, formatValidatorLabel(null))

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    val toastEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    val canContinue: StateFlow<Boolean> = combine(
        _selectedValidatorHotkey,
        _amount,
        _submitting,
    ) { hotkey, amt, submitting ->
        !submitting && hotkey != null && amt.isNotBlank() && (amt.toDoubleOrNull() ?: 0.0) > 0.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Validator picker reports its selection by stuffing the chosen
        // hotkey into our nav back-stack entry's saved state. The router
        // exposes that as a flow — same pattern as the crowdloan-bonus
        // round-trip.
        router.subtensorSelectedValidatorFlow
            .filterNotNull()
            .onEach { hotkey -> _selectedValidatorHotkey.value = hotkey }
            .launchIn(viewModelScope)
    }

    fun amountChanged(text: String) {
        _amount.value = text
    }

    fun selectValidatorClicked() {
        router.openSubtensorValidatorPicker(netuid)
    }

    fun continueClicked() {
        if (_submitting.value) return
        val hotkeyAddress = _selectedValidatorHotkey.value ?: return
        val amountTao = _amount.value.toBigDecimalOrNull() ?: return
        if (amountTao <= BigDecimal.ZERO) return

        submitStake(hotkeyAddress, amountTao)
    }

    fun backClicked() {
        if (_submitting.value) return
        router.back()
    }

    private fun submitStake(hotkeyAddress: String, amountTao: BigDecimal) {
        _submitting.value = true
        viewModelScope.launch {
            val precision = runCatching { stakingSharedState.chainAsset().precision.value.toInt() }.getOrDefault(9)
            val planks = amountTao
                .multiply(BigDecimal.TEN.pow(precision))
                .toBigInteger()

            val hotkeyBytes = runCatching { hotkeyAddress.toAccountId() }.getOrNull()
            if (hotkeyBytes == null) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_setup_invalid_validator))
                return@launch
            }

            val result = submitInteractor.submitStake(
                netuid = netuid,
                hotkey = hotkeyBytes,
                amountInPlanks = planks,
            )
            _submitting.value = false
            result.fold(
                onSuccess = {
                    // Pop straight back to the multistaking dashboard. The
                    // dashboard's next sync (cache was just invalidated)
                    // surfaces the new position automatically.
                    dashboardRouter.returnToStakingDashboard()
                },
                onFailure = { error ->
                    val message = error.localizedMessage
                        ?: resourceManager.getString(R.string.subtensor_setup_submit_failed)
                    toastEvents.value = Event(message)
                },
            )
        }
    }

    private fun resolveTitle(): String = when {
        isRoot -> resourceManager.getString(R.string.subtensor_stake_more)
        !subnetName.isNullOrBlank() -> subnetName
        else -> "Subnet $netuid"
    }

    private fun formatValidatorLabel(hotkey: String?): String {
        if (hotkey.isNullOrBlank()) {
            return resourceManager.getString(R.string.subtensor_setup_select_validator)
        }
        return if (hotkey.length <= 10) hotkey else "${hotkey.take(6)}…${hotkey.takeLast(4)}"
    }
}
