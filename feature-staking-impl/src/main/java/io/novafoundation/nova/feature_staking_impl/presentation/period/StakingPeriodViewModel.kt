package io.novafoundation.nova.feature_staking_impl.presentation.period

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatting.formatDateSinceEpoch
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class CustomPeriod(
    val start: Long?,
    val end: Long?,
    val isEndToday: Boolean
) {
    constructor() : this(null, null, true)
}

class DateRangeWithCurrent(
    var current: Long,
    var start: Long?,
    var end: Long
)

class StakingPeriodViewModel(
    private val stakingRewardPeriodInteractor: StakingRewardPeriodInteractor,
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
        val currentPeriod = stakingRewardPeriodInteractor.getRewardPeriod()
        selectedPeriod != null && selectedPeriod != currentPeriod
    }
        .shareInBackground()

    init {
        launch {
            val rewardPeriod = stakingRewardPeriodInteractor.getRewardPeriod()

            if (rewardPeriod is RewardPeriod.Custom) {
                _customPeriod.value = mapCustomPeriod(rewardPeriod)
            }

            selectedPeriod.value = mapStackingPeriodToIdRes(rewardPeriod)
        }
    }

    fun onSaveClick() {
        val result = mapSelectedPeriod(selectedPeriod.value, _customPeriod.value)
        result?.let { stakingRewardPeriodInteractor.setRewardPeriod(it) }
        router.back()
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
        return when (rewardPeriod) {
            RewardPeriod.All -> R.id.allTimeStakingPeriod
            RewardPeriod.Week -> R.id.weekStakingPeriod
            RewardPeriod.Month -> R.id.monthStakingPeriod
            RewardPeriod.Quarter -> R.id.quarterStakingPeriod
            RewardPeriod.HalfYear -> R.id.halfYearStakingPeriod
            RewardPeriod.Year -> R.id.yearStakingPeriod
            is RewardPeriod.Custom -> R.id.customStakingPeriod
        }
    }

    private fun mapSelectedPeriod(@IdRes periodId: Int, customPeriod: CustomPeriod?): RewardPeriod? {
        return when (periodId) {
            R.id.allTimeStakingPeriod -> RewardPeriod.All
            R.id.weekStakingPeriod -> RewardPeriod.Week
            R.id.monthStakingPeriod -> RewardPeriod.Month
            R.id.quarterStakingPeriod -> RewardPeriod.Quarter
            R.id.halfYearStakingPeriod -> RewardPeriod.HalfYear
            R.id.yearStakingPeriod -> RewardPeriod.Year
            R.id.customStakingPeriod -> customPeriod?.let { mapCustomPeriod(it) }
            else -> throw IllegalArgumentException("Unknown idRes: $periodId")
        }
    }

    private fun mapCustomPeriod(customPeriod: CustomPeriod): RewardPeriod.Custom? {
        val start = customPeriod.start ?: return null

        val endPeriod = if (customPeriod.isEndToday) {
            RewardPeriod.TimePoint.NoThreshold
        } else {
            val end = customPeriod.end ?: return null
            RewardPeriod.TimePoint.Threshold(end)
        }
        return RewardPeriod.Custom(RewardPeriod.TimePoint.Threshold(start), endPeriod)
    }

    private fun mapCustomPeriod(period: RewardPeriod.Custom): CustomPeriod {
        val start = period.start as RewardPeriod.TimePoint.Threshold
        val end = period.end as? RewardPeriod.TimePoint.Threshold
        val isEndToday = period.end is RewardPeriod.TimePoint.NoThreshold

        return CustomPeriod(
            start = start.millis,
            end = end?.millis,
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
