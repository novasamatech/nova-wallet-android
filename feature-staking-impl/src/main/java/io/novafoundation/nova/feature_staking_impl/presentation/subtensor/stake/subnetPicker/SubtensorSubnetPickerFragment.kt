package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker

import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorSubnetPickerBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorSubnetPickerFragment : BaseFragment<SubtensorSubnetPickerViewModel, FragmentSubtensorSubnetPickerBinding>() {

    private var filterIconView: android.widget.ImageView? = null

    override fun createBinding() = FragmentSubtensorSubnetPickerBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorSubnetPickerToolbar.setHomeButtonListener { viewModel.backClicked() }

        // Mirrors iOS subnet picker `setupNavigationBar`: filter rightmost,
        // search to its left.
        filterIconView = binder.subtensorSubnetPickerToolbar.addCustomAction(R.drawable.ic_filter) {
            showFilterSheet()
        }
        binder.subtensorSubnetPickerToolbar.addCustomAction(R.drawable.ic_search) {
            toggleSearchVisibility()
        }

        binder.subtensorSubnetPickerSearch.content.onTextChanged { text ->
            viewModel.searchQueryChanged(text.toString())
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorSubnetPickerFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorSubnetPickerViewModel) {
        viewModel.state.observe { state -> renderState(state) }
        viewModel.filterState.observe { state -> renderFilterIcon(state) }
    }

    private fun renderState(state: SubtensorSubnetPickerViewModel.UiState) {
        binder.subtensorSubnetPickerProgress.isVisible = state is SubtensorSubnetPickerViewModel.UiState.Loading
        binder.subtensorSubnetPickerEmpty.isVisible = state is SubtensorSubnetPickerViewModel.UiState.Empty
        binder.subtensorSubnetPickerList.isVisible = state is SubtensorSubnetPickerViewModel.UiState.Loaded
        if (state is SubtensorSubnetPickerViewModel.UiState.Loaded) {
            renderRows(state.rows)
        }
    }

    private fun renderRows(rows: List<SubtensorSubnetPickerViewModel.SubnetRow>) {
        val container = binder.subtensorSubnetPickerList
        container.removeAllViews()
        rows.forEach { row ->
            val view = layoutInflater.inflate(R.layout.item_subtensor_subnet_picker, container, false)
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowName).text = row.displayName
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowNetuid).text = row.netuidLabel
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowReserve).text = row.taoReserveText
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowPrice).text = row.priceText
            view.setOnClickListener { viewModel.subnetClicked(row.netuid) }
            (container as LinearLayout).addView(view)
        }
    }

    private fun toggleSearchVisibility() {
        val search = binder.subtensorSubnetPickerSearch
        if (search.isVisible) {
            search.isVisible = false
            search.content.text.clear()
        } else {
            search.isVisible = true
            search.content.showSoftKeyboard()
        }
    }

    private fun renderFilterIcon(state: SubtensorSubnetPickerViewModel.FilterState) {
        val iconRes = if (state.isDefault) R.drawable.ic_filter else R.drawable.ic_filter_indicator
        filterIconView?.setImageResource(iconRes)
    }

    private fun showFilterSheet() {
        val current = viewModel.filterState.value
        val dialog = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.bottom_sheet_subtensor_subnet_filter, null)

        val stakeRadio = content.findViewById<RadioButton>(R.id.subtensorSubnetFilterSortStakeRadio)
        val netuidRadio = content.findViewById<RadioButton>(R.id.subtensorSubnetFilterSortNetuidRadio)
        val stakeRow = content.findViewById<View>(R.id.subtensorSubnetFilterSortStakeRow)
        val netuidRow = content.findViewById<View>(R.id.subtensorSubnetFilterSortNetuidRow)
        val resetView = content.findViewById<TextView>(R.id.subtensorSubnetFilterReset)
        val applyButton = content.findViewById<View>(R.id.subtensorSubnetFilterApply)

        var working = current
        fun render() {
            stakeRadio.isChecked = working.sort == SubtensorSubnetPickerViewModel.Sort.TOTAL_STAKE_DESC
            netuidRadio.isChecked = working.sort == SubtensorSubnetPickerViewModel.Sort.NETUID_ASC
        }
        render()

        stakeRow.setOnClickListener {
            working = working.copy(sort = SubtensorSubnetPickerViewModel.Sort.TOTAL_STAKE_DESC)
            render()
        }
        netuidRow.setOnClickListener {
            working = working.copy(sort = SubtensorSubnetPickerViewModel.Sort.NETUID_ASC)
            render()
        }
        resetView.setOnClickListener {
            working = SubtensorSubnetPickerViewModel.FilterState.DEFAULT
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
