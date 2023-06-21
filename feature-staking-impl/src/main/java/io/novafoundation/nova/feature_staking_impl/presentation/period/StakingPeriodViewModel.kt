package io.novafoundation.nova.feature_staking_impl.presentation.period

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.reversed
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import io.novafoundation.nova.runtime.state.selectedOption
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val PERIOD_TYPES_TO_IDS = mapOf(
    RewardPeriodType.ALL_TIME to R.id.allTimeStakingPeriod,
    RewardPeriodType.WEEK to R.id.weekStakingPeriod,
    RewardPeriodType.MONTH to R.id.monthStakingPeriod,
    RewardPeriodType.QUARTER to R.id.quarterStakingPeriod,
    RewardPeriodType.HALF_YEAR to R.id.halfYearStakingPeriod,
    RewardPeriodType.YEAR to R.id.yearStakingPeriod,
    RewardPeriodType.CUSTOM to R.id.customStakingPeriod
)

private val IDS_TO_PERIOD_TYPES = PERIOD_TYPES_TO_IDS.reversed()

data class CustomPeriod(
    val start: Long? = null,
    val end: Long? = null,
    val isEndToday: Boolean = true
)

class DateRangeWithCurrent(
    var current: Long,
    var start: Long?,
    var end: Long
)

class StakingPeriodViewModel(
    private val stakingRewardPeriodInteractor: StakingRewardPeriodInteractor,
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter
) : BaseViewModel() {

    private val _startDatePickerEvent = MutableLiveData<Event<DateRangeWithCurrent>>()
    val startDatePickerEvent: LiveData<Event<DateRangeWithCurrent>> = _startDatePickerEvent

    private val _endDatePickerEvent = MutableLiveData<Event<DateRangeWithCurrent>>()
    val endDatePickerEvent: LiveData<Event<DateRangeWithCurrent>> = _endDatePickerEvent

    val selectedPeriod: MutableStateFlow<Int> = MutableStateFlow(R.id.allTimeStakingPeriod)
    private val _customPeriod: MutableStateFlow<CustomPeriod> = MutableStateFlow(CustomPeriod())

    val showCustomDetails: Flow<Boolean> = selectedPeriod
        .map { it == R.id.customStakingPeriod }
        .shareInBackground()

    val startCustomPeriod = _customPeriod
        .map { mapMillisToStrDate(it.start) }
        .shareInBackground()

    val endCustomPeriod = _customPeriod
        .map { mapMillisToStrDate(it.end) }
        .shareInBackground()

    val endIsToday = _customPeriod
        .map { it.isEndToday }
        .shareInBackground()

    val saveButtonEnabledState: Flow<Boolean> = combine(selectedPeriod, _customPeriod) { id, customPeriod ->
        val selectedPeriod = mapSelectedPeriod(id, customPeriod)
        val currentPeriod = getSelectedRewardPeriod()
        selectedPeriod != null && selectedPeriod != currentPeriod
    }
        .shareInBackground()

    init {
        launch {
            val rewardPeriod = getSelectedRewardPeriod()

            if (rewardPeriod is RewardPeriod.CustomRange) {
                _customPeriod.value = mapCustomPeriodFromEntity(rewardPeriod)
            }

            selectedPeriod.value = mapStackingPeriodToIdRes(rewardPeriod)
        }
    }

    private suspend fun getSelectedRewardPeriod(): RewardPeriod {
        val chain = stakingSharedState.chain()
        val chainAsset = stakingSharedState.chainAsset()
        val stakingType = stakingSharedState.selectedOption().additional.stakingType
        return stakingRewardPeriodInteractor.getRewardPeriod(chain, chainAsset, stakingType)
    }

    fun onSaveClick() {
        launch {
            val chain = stakingSharedState.chain()
            val chainAsset = stakingSharedState.chainAsset()
            val stakingType = stakingSharedState.selectedOption().additional.stakingType
            val result = mapSelectedPeriod(selectedPeriod.value, _customPeriod.value)
            result?.let { stakingRewardPeriodInteractor.setRewardPeriod(chain, chainAsset, stakingType, it) }
            router.back()
        }
    }

    fun endIsAlwaysTodayChanged(isAlwaysToday: Boolean) {
        val customPeriod = _customPeriod.value
        _customPeriod.value = customPeriod.copy(end = null, isEndToday = isAlwaysToday)
    }

    fun startDateSelected(value: Long) {
        val customPeriod = _customPeriod.value
        _customPeriod.value = customPeriod.copy(start = value)
    }

    fun endDateSelected(value: Long) {
        val customPeriod = _customPeriod.value
        _customPeriod.value = customPeriod.copy(end = value)
    }

    fun openStartDatePicker() {
        val customPeriod = _customPeriod.value
        val currentSelectedStartTime = customPeriod.start ?: System.currentTimeMillis()
        val endValidPeriod = customPeriod.end?.minus(TimeUnit.DAYS.toMillis(1)) ?: System.currentTimeMillis()
        val dateRange = DateRangeWithCurrent(currentSelectedStartTime, null, endValidPeriod)
        _startDatePickerEvent.value = dateRange.event()
    }

    fun openEndDatePicker() {
        val customPeriod = _customPeriod.value
        val currentSelectedEndTime = customPeriod.end ?: System.currentTimeMillis()
        val startValidPeriod = customPeriod.start?.plus(TimeUnit.DAYS.toMillis(1))
        val endValidPeriod = System.currentTimeMillis()
        val dateRange = DateRangeWithCurrent(currentSelectedEndTime, startValidPeriod, endValidPeriod)
        _endDatePickerEvent.value = dateRange.event()
    }

    private fun mapStackingPeriodToIdRes(rewardPeriod: RewardPeriod): Int {
        return PERIOD_TYPES_TO_IDS.getValue(rewardPeriod.type)
    }

    private fun mapSelectedPeriod(@IdRes periodId: Int, customPeriod: CustomPeriod?): RewardPeriod? {
        return when {
            periodId == R.id.allTimeStakingPeriod -> RewardPeriod.AllTime
            periodId == R.id.customStakingPeriod -> mapCustomPeriodToEntity(customPeriod)
            IDS_TO_PERIOD_TYPES.containsKey(periodId) -> {
                val type = IDS_TO_PERIOD_TYPES.getValue(periodId)
                RewardPeriod.OffsetFromCurrent(RewardPeriod.getOffsetByType(type), type)
            }
            else -> null
        }
    }

    private fun mapCustomPeriodToEntity(customPeriod: CustomPeriod?): RewardPeriod.CustomRange? {
        if (customPeriod == null) return null

        val startPeriod = customPeriod.start?.let { Date(it) } ?: return null

        val endPeriod = if (customPeriod.isEndToday) {
            null
        } else {
            val end = customPeriod.end ?: return null
            Date(end)
        }

        return RewardPeriod.CustomRange(startPeriod, endPeriod)
    }

    private fun mapCustomPeriodFromEntity(period: RewardPeriod.CustomRange): CustomPeriod {
        val isEndToday = period.end == null

        return CustomPeriod(
            start = period.start.time,
            end = period.end?.time,
            isEndToday = isEndToday
        )
    }

    private fun mapMillisToStrDate(millis: Long?): String {
        return millis?.let { resourceManager.formatDate(it) }
            ?: resourceManager.getString(R.string.staking_period_select_date)
    }

    fun backClicked() {
        router.back()
    }
}
