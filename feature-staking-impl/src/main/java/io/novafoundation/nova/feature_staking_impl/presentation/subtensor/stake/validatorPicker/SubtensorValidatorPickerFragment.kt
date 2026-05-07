package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorValidatorPickerBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorValidatorPickerFragment : BaseFragment<SubtensorValidatorPickerViewModel, FragmentSubtensorValidatorPickerBinding>() {

    companion object {
        private const val ARG_NETUID = "netuid"

        fun bundle(netuid: Int): Bundle = Bundle().apply { putInt(ARG_NETUID, netuid) }
    }

    private var filterIconView: android.widget.ImageView? = null

    override fun createBinding() = FragmentSubtensorValidatorPickerBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorValidatorPickerToolbar.setHomeButtonListener { viewModel.backClicked() }

        // Mirrors iOS `setupNavigationBar`: filter rightmost, then search.
        // `addCustomAction` adds at index 0, so first call lands rightmost.
        filterIconView = binder.subtensorValidatorPickerToolbar.addCustomAction(R.drawable.ic_filter) {
            showFilterSheet()
        }
        binder.subtensorValidatorPickerToolbar.addCustomAction(R.drawable.ic_search) {
            toggleSearchVisibility()
        }

        binder.subtensorValidatorPickerSearch.content.onTextChanged { text ->
            viewModel.searchQueryChanged(text.toString())
        }
    }

    override fun inject() {
        val netuid = arguments?.getInt(ARG_NETUID) ?: 0
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorValidatorPickerFactory()
            .create(this, netuid)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorValidatorPickerViewModel) {
        viewModel.state.observe { state -> renderState(state) }
        viewModel.filterState.observe { state -> renderFilterIcon(state) }
    }

    private fun renderState(state: SubtensorValidatorPickerViewModel.UiState) {
        binder.subtensorValidatorPickerProgress.isVisible = state is SubtensorValidatorPickerViewModel.UiState.Loading
        binder.subtensorValidatorPickerEmpty.isVisible = state is SubtensorValidatorPickerViewModel.UiState.Empty
        binder.subtensorValidatorPickerError.isVisible = state is SubtensorValidatorPickerViewModel.UiState.Error
        binder.subtensorValidatorPickerList.isVisible = state is SubtensorValidatorPickerViewModel.UiState.Loaded
        if (state is SubtensorValidatorPickerViewModel.UiState.Loaded) {
            renderRows(state.rows)
        }
    }

    private fun renderRows(rows: List<SubtensorValidatorPickerViewModel.ValidatorRow>) {
        val container = binder.subtensorValidatorPickerList
        container.removeAllViews()
        rows.forEach { row ->
            val view = layoutInflater.inflate(R.layout.item_subtensor_validator_picker, container, false)
            val iconView = view.findViewById<android.widget.ImageView>(R.id.subtensorValidatorPickerRowIcon)
            iconView.setImageDrawable(row.identicon)
            iconView.isVisible = row.identicon != null

            val nameView = view.findViewById<TextView>(R.id.subtensorValidatorPickerRowName)
            nameView.text = row.displayName
            nameView.typeface = if (row.isHotkeyDisplayed) {
                android.graphics.Typeface.MONOSPACE
            } else {
                android.graphics.Typeface.DEFAULT
            }

            view.findViewById<TextView>(R.id.subtensorValidatorPickerRowApr).text = row.aprText
            view.findViewById<TextView>(R.id.subtensorValidatorPickerRowCommission).apply {
                text = row.commissionText
                isVisible = row.commissionText.isNotEmpty()
            }
            view.setOnClickListener { viewModel.rowClicked(row.hotkey) }
            (container as LinearLayout).addView(view)
        }
    }

    private fun toggleSearchVisibility() {
        val search = binder.subtensorValidatorPickerSearch
        if (search.isVisible) {
            search.isVisible = false
            search.content.text.clear()
            // Search query reset is implicit via the EditText listener
        } else {
            search.isVisible = true
            search.content.showSoftKeyboard()
        }
    }

    /**
     * Filter icon stays the default outline when state is at default; flips
     * to `ic_filter_indicator` (the dotted-active variant) when ANY toggle
     * or the sort is non-default. Mirrors iOS `refreshFilterButtonStyle`.
     */
    private fun renderFilterIcon(state: SubtensorValidatorPickerViewModel.FilterState) {
        val iconRes = if (state.isDefault) R.drawable.ic_filter else R.drawable.ic_filter_indicator
        filterIconView?.setImageResource(iconRes)
    }

    private fun showFilterSheet() {
        val current = viewModel.filterState.value
        val dialog = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.bottom_sheet_subtensor_validator_filter, null)

        val identitySwitch = content.findViewById<SwitchMaterial>(R.id.subtensorValidatorFilterIdentitySwitch)
        val commissionSwitch = content.findViewById<SwitchMaterial>(R.id.subtensorValidatorFilterCommissionSwitch)
        val aprRadio = content.findViewById<RadioButton>(R.id.subtensorValidatorFilterSortAprRadio)
        val stakeRadio = content.findViewById<RadioButton>(R.id.subtensorValidatorFilterSortStakeRadio)
        val identityRow = content.findViewById<View>(R.id.subtensorValidatorFilterIdentityRow)
        val commissionRow = content.findViewById<View>(R.id.subtensorValidatorFilterCommissionRow)
        val aprRow = content.findViewById<View>(R.id.subtensorValidatorFilterSortAprRow)
        val stakeRow = content.findViewById<View>(R.id.subtensorValidatorFilterSortStakeRow)
        val resetView = content.findViewById<TextView>(R.id.subtensorValidatorFilterReset)
        val applyButton = content.findViewById<View>(R.id.subtensorValidatorFilterApply)

        // Mutable working copy of the filter state — only published on Apply.
        var working = current
        fun render() {
            identitySwitch.isChecked = working.requireIdentity
            commissionSwitch.isChecked = working.hideMaxCommission
            aprRadio.isChecked = working.sort == SubtensorValidatorPickerViewModel.Sort.APR_DESC
            stakeRadio.isChecked = working.sort == SubtensorValidatorPickerViewModel.Sort.TOTAL_STAKE_DESC
        }
        render()

        identityRow.setOnClickListener {
            working = working.copy(requireIdentity = !working.requireIdentity)
            render()
        }
        commissionRow.setOnClickListener {
            working = working.copy(hideMaxCommission = !working.hideMaxCommission)
            render()
        }
        aprRow.setOnClickListener {
            working = working.copy(sort = SubtensorValidatorPickerViewModel.Sort.APR_DESC)
            render()
        }
        stakeRow.setOnClickListener {
            working = working.copy(sort = SubtensorValidatorPickerViewModel.Sort.TOTAL_STAKE_DESC)
            render()
        }
        resetView.setOnClickListener {
            working = SubtensorValidatorPickerViewModel.FilterState.DEFAULT
            render()
        }
        applyButton.setOnClickListener {
            viewModel.filterChanged(working)
            dialog.dismiss()
        }

        dialog.setContentView(content)
        dialog.show()
    }
}
