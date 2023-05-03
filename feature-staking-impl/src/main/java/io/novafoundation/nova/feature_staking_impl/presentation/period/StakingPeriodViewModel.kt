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

class StakingPeriodViewModel : BaseViewModel() {

    private val _selectedPeriod: MutableStateFlow<StackingPeriod> = MutableStateFlow(StackingPeriod.AllTime)
    val selectedPeriod: Flow<Int> = _selectedPeriod
        .map { mapStackingPeriodToIdRes(it) }
        .shareInBackground()

    val saveButtonEnabledState: Flow<Boolean> = emptyFlow()

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
            R.id.customStackingPeriod -> TODO()
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

    fun backClicked() {
        //TODO
    }
}
