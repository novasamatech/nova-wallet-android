package io.novafoundation.nova.feature_staking_impl.presentation.period

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.RangeDateValidator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentPeriodStakingBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val DATE_PICKER_TAG = "datePicker"

class StakingPeriodFragment : BaseFragment<StakingPeriodViewModel, FragmentPeriodStakingBinding>() {

    override val binder by viewBinding(FragmentPeriodStakingBinding::bind)

    override fun initViews() {
        binder.stakingPeriodToolbar.applyStatusBarInsets()
        binder.stakingPeriodToolbar.setRightActionClickListener { viewModel.onSaveClick() }
        binder.stakingPeriodToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.customStakingPeriodStart.setOnClickListener { viewModel.openStartDatePicker() }
        binder.customStakingPeriodEnd.setOnClickListener { viewModel.openEndDatePicker() }
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
        binder.stakingPeriodGroup.bindTo(viewModel.selectedPeriod, this.lifecycleScope)
        binder.customStakingPeriodAlwaysToday.bindTo(viewModel.endIsToday, this.lifecycleScope, viewModel::endIsAlwaysTodayChanged)

        viewModel.startDatePickerEvent.observeEvent {
            startDatePicker(R.string.staking_period_start_date_picker_title, it, ::onStartDateSelected)
        }

        viewModel.endDatePickerEvent.observeEvent {
            startDatePicker(R.string.staking_period_end_date_picker_title, it, ::onEndDateSelected)
        }

        viewModel.startCustomPeriod.observe {
            binder.customStakingPeriodStart.showValue(it)
        }

        viewModel.showCustomDetails.observe {
            binder.customPeriodSettings.isVisible = it
        }

        viewModel.endIsToday.observe {
            binder.customStakingPeriodEnd.isVisible = !it
        }

        viewModel.endCustomPeriod.observe {
            binder.customStakingPeriodEnd.showValue(it)
        }

        viewModel.saveButtonEnabledState.observe {
            binder.stakingPeriodToolbar.setRightActionEnabled(it)
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
