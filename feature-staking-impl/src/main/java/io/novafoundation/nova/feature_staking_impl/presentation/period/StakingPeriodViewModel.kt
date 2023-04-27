package io.novafoundation.nova.feature_staking_impl.presentation.period

import androidx.annotation.IdRes
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.period.StackingPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CustomPeriod(
    val start: String?,
    val end: String?,
    val isEndVisible: Boolean,
)

class StakingPeriodViewModel : BaseViewModel() {

    private val _selectedPeriod: MutableStateFlow<StackingPeriod> = MutableStateFlow(StackingPeriod.AllTime)
    val selectedPeriod: Flow<Int> = _selectedPeriod
        .map { mapStackingPeriodToIdRes(it) }
        .shareInBackground()

    private val _alwaysTodayEndPeriodEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val alwaysTodayEndPeriodEnabled: Flow<Boolean> = _alwaysTodayEndPeriodEnabled

    private val _customPeriod: MutableStateFlow<StackingPeriod.Custom?> = MutableStateFlow(null)

    val customPeriod: Flow<CustomPeriod?> = combine(
        _alwaysTodayEndPeriodEnabled,
        _selectedPeriod
    ) { alwaysTodayEndPeriodEnabled, selectedPeriod ->

    }

    val saveButtonEnabledState: Flow<Boolean> = emptyFlow()

    private val oldCustomPeriod: StackingPeriod.Custom? = null

    init {
        launch {
            //retrieve current state from repository
        }
    }

    fun onPeriodChanged(@IdRes checkedId: Int) {
        _selectedPeriod.value = mapIdResToStackingPeriod(checkedId)
    }

    fun onSaveClick() {

    }

    private fun mapIdResToStackingPeriod(@IdRes idRes: Int): StackingPeriod {
        return when (idRes) {
            R.id.allTimeStackingPeriod -> StackingPeriod.AllTime
            R.id.weekStackingPeriod -> StackingPeriod.Week
            R.id.monthStackingPeriod -> StackingPeriod.Month
            R.id.quarterStackingPeriod -> StackingPeriod.Quarter
            R.id.halfYearStackingPeriod -> StackingPeriod.HalfYear
            R.id.yearStackingPeriod -> StackingPeriod.Year
            R.id.customStackingPeriod -> StackingPeriod.Custom("", "")
            else -> throw IllegalArgumentException("Unknown idRes: $idRes")
        }
    }

    private fun mapStackingPeriodToIdRes(stackingPeriod: StackingPeriod): Int {
        return when (stackingPeriod) {
            StackingPeriod.AllTime -> R.id.allTimeStackingPeriod
            StackingPeriod.Week -> R.id.weekStackingPeriod
            StackingPeriod.Month -> R.id.monthStackingPeriod
            StackingPeriod.Quarter -> R.id.quarterStackingPeriod
            StackingPeriod.HalfYear -> R.id.halfYearStackingPeriod
            StackingPeriod.Year -> R.id.yearStackingPeriod
            is StackingPeriod.Custom -> R.id.customStackingPeriod
        }
    }
}
