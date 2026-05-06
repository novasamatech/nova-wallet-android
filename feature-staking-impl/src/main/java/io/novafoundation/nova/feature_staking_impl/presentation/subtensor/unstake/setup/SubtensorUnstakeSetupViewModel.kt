package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

class SubtensorUnstakeSetupViewModel(
    private val router: StakingRouter,
    private val dashboardRouter: StakingDashboardRouter,
    private val resourceManager: ResourceManager,
    private val stakingSharedState: StakingSharedState,
    private val submitInteractor: SubtensorStakeSubmitInteractor,
    val netuid: Int,
    val hotkeyAddress: String,
    val positionPlanks: BigInteger,
) : BaseViewModel() {

    val isRoot: Boolean = netuid == SubtensorStakingConstants.ROOT_NETUID

    val titleText: StateFlow<String> = MutableStateFlow(resourceManager.getString(R.string.subtensor_unstake)).asStateFlow()

    val validatorLabel: StateFlow<String> = MutableStateFlow(formatValidatorLabel()).asStateFlow()

    /** Resolved once chainAsset() returns. Default 9 (Bittensor mainnet TAO). */
    private val precision = MutableStateFlow(9)

    val positionLabel: StateFlow<String> = precision
        .map { formatPosition(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, formatPosition(9))

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    val toastEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    val canContinue: StateFlow<Boolean> = combine(_amount, _submitting) { amt, submitting ->
        !submitting && amt.isNotBlank() && (amt.toBigDecimalOrNull()?.signum() ?: 0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            precision.value = runCatching {
                stakingSharedState.chainAsset().precision.value.toInt()
            }.getOrDefault(9)
        }
    }

    fun amountChanged(text: String) {
        _amount.value = text
    }

    fun maxClicked() {
        // Mirrors iOS UnstakeSetupPresenter.availableForMax — full position
        // amount as a decimal so the input shows the user's complete stake.
        val tao = positionPlanks.toBigDecimal().movePointLeft(precision.value).stripTrailingZeros().toPlainString()
        _amount.value = tao
    }

    fun continueClicked() {
        if (_submitting.value) return
        val amt = _amount.value.toBigDecimalOrNull() ?: return
        if (amt <= BigDecimal.ZERO) return
        submit(amt)
    }

    fun backClicked() {
        if (_submitting.value) return
        router.back()
    }

    private fun submit(amountInUnits: BigDecimal) {
        _submitting.value = true
        viewModelScope.launch {
            val planks = amountInUnits.multiply(BigDecimal.TEN.pow(precision.value)).toBigInteger()
            if (planks > positionPlanks) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_unstake_amount_exceeds))
                return@launch
            }

            val hotkeyBytes: ByteArray? = runCatching { hotkeyAddress.toAccountId() }.getOrNull()
            if (hotkeyBytes == null) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_setup_invalid_validator))
                return@launch
            }

            val result = submitInteractor.submitUnstake(
                netuid = netuid,
                hotkey = hotkeyBytes,
                amountInPlanks = planks,
            )
            _submitting.value = false
            result.fold(
                onSuccess = { dashboardRouter.returnToStakingDashboard() },
                onFailure = { error ->
                    val message = error.localizedMessage
                        ?: resourceManager.getString(R.string.subtensor_setup_submit_failed)
                    toastEvents.value = Event(message)
                },
            )
        }
    }

    private fun formatValidatorLabel(): String =
        if (hotkeyAddress.length <= 10) hotkeyAddress else "${hotkeyAddress.take(6)}…${hotkeyAddress.takeLast(4)}"

    private fun formatPosition(precision: Int): String {
        val tao = positionPlanks.toBigDecimal().movePointLeft(precision)
        val symbol = if (isRoot) "TAO" else "α"
        return "%.4f %s".format(tao, symbol)
    }
}
