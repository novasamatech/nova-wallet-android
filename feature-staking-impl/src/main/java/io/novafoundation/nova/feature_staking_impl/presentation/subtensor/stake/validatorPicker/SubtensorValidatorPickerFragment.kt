package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorValidatorPickerBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorValidatorPickerFragment : BaseFragment<SubtensorValidatorPickerViewModel, FragmentSubtensorValidatorPickerBinding>() {

    companion object {
        private const val ARG_NETUID = "netuid"

        fun bundle(netuid: Int): Bundle = Bundle().apply { putInt(ARG_NETUID, netuid) }
    }

    override fun createBinding() = FragmentSubtensorValidatorPickerBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorValidatorPickerToolbar.setHomeButtonListener { viewModel.backClicked() }
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
        rows.forEachIndexed { index, row ->
            val view = layoutInflater.inflate(R.layout.item_subtensor_validator_picker, container, false)
            view.findViewById<TextView>(R.id.subtensorValidatorPickerRowName).text = row.displayName
            view.findViewById<TextView>(R.id.subtensorValidatorPickerRowHotkey).apply {
                text = row.hotkeyShort
                isVisible = row.displayName != row.hotkeyShort
            }
            view.findViewById<TextView>(R.id.subtensorValidatorPickerRowStake).text = row.totalStakeText
            view.findViewById<TextView>(R.id.subtensorValidatorPickerRowMeta).apply {
                val parts = listOf(row.aprText, row.commissionText).filter { it.isNotEmpty() }
                text = parts.joinToString(" · ")
                isVisible = parts.isNotEmpty()
            }
            view.findViewById<View>(R.id.subtensorValidatorPickerRowDivider).visibility =
                if (index < rows.size - 1) View.VISIBLE else View.GONE
            view.setOnClickListener { viewModel.rowClicked(row.hotkey) }
            (container as LinearLayout).addView(view)
        }
    }
}
