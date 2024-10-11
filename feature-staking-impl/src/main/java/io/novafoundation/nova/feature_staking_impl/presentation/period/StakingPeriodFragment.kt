package io.novafoundation.nova.feature_staking_impl.presentation.period

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.RangeDateValidator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val DATE_PICKER_TAG = "datePicker"

class StakingPeriodFragment : BaseFragment<StakingPeriodViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_period_staking, container, false)
    }

    override fun initViews() {
        stakingPeriodToolbar.applyStatusBarInsets()
        stakingPeriodToolbar.setRightActionClickListener { viewModel.onSaveClick() }
        stakingPeriodToolbar.setHomeButtonListener { viewModel.backClicked() }
        customStakingPeriodStart.setOnClickListener { viewModel.openStartDatePicker() }
        customStakingPeriodEnd.setOnClickListener { viewModel.openEndDatePicker() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .stakingPeriodComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StakingPeriodViewModel) {
        stakingPeriodGroup.bindTo(viewModel.selectedPeriod, this.lifecycleScope)
        customStakingPeriodAlwaysToday.bindTo(viewModel.endIsToday, this.lifecycleScope, viewModel::endIsAlwaysTodayChanged)

        viewModel.startDatePickerEvent.observeEvent {
            startDatePicker(R.string.staking_period_start_date_picker_title, it, ::onStartDateSelected)
        }

        viewModel.endDatePickerEvent.observeEvent {
            startDatePicker(R.string.staking_period_end_date_picker_title, it, ::onEndDateSelected)
        }

        viewModel.startCustomPeriod.observe {
            customStakingPeriodStart.showValue(it)
        }

        viewModel.showCustomDetails.observe {
            customPeriodSettings.isVisible = it
        }

        viewModel.endIsToday.observe {
            customStakingPeriodEnd.isVisible = !it
        }

        viewModel.endCustomPeriod.observe {
            customStakingPeriodEnd.showValue(it)
        }

        viewModel.saveButtonEnabledState.observe {
            stakingPeriodToolbar.setRightActionEnabled(it)
        }
    }

    private fun startDatePicker(@StringRes title: Int, date: DateRangeWithCurrent, onDateSelected: (Long) -> Unit) {
        val calendarConstraints = CalendarConstraints.Builder()
            .setEnd(date.end)
            .setOpenAt(date.current)
            .setValidator(RangeDateValidator(date.start, date.end))
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(date.current)
            .setCalendarConstraints(calendarConstraints)
            .build()

        datePicker.addOnPositiveButtonClickListener(onDateSelected)
        datePicker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    private fun onStartDateSelected(value: Long) {
        viewModel.startDateSelected(value)
    }

    private fun onEndDateSelected(value: Long) {
        viewModel.endDateSelected(value)
    }
}
