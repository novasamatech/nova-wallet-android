package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorSubnetPickerBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorSubnetPickerFragment : BaseFragment<SubtensorSubnetPickerViewModel, FragmentSubtensorSubnetPickerBinding>() {

    override fun createBinding() = FragmentSubtensorSubnetPickerBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorSubnetPickerToolbar.setHomeButtonListener { viewModel.backClicked() }
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
        rows.forEachIndexed { index, row ->
            val view = layoutInflater.inflate(R.layout.item_subtensor_subnet_picker, container, false)
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowName).text = row.displayName
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowNetuid).text = row.netuidLabel
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowReserve).text = row.taoReserveText
            view.findViewById<TextView>(R.id.subtensorSubnetPickerRowPrice).text = row.priceText
            view.findViewById<View>(R.id.subtensorSubnetPickerRowDivider).visibility =
                if (index < rows.size - 1) View.VISIBLE else View.GONE
            view.setOnClickListener { viewModel.subnetClicked(row.netuid) }
            (container as LinearLayout).addView(view)
        }
    }
}
