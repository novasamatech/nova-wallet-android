package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSubtensorStakingBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class SubtensorStakingFragment : BaseFragment<SubtensorStakingViewModel, FragmentSubtensorStakingBinding>() {

    private var hasAppeared: Boolean = false

    override fun createBinding() = FragmentSubtensorStakingBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.subtensorStakingToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.subtensorStakeMore.setOnClickListener { viewModel.stakeMore() }
        binder.subtensorUnstake.setOnClickListener { viewModel.unstake() }
    }

    override fun onResume() {
        super.onResume()

        // Initial load is driven by the ViewModel's `init` block. Subsequent
        // appearances (e.g. popping back from the stake-confirm flow once that
        // lands) need a refresh so the new position lands without making the
        // user re-open the screen. Mirrors iOS `viewWillAppear` semantics.
        if (hasAppeared) {
            viewModel.refresh()
        } else {
            hasAppeared = true
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java,
        )
            .subtensorStakingFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SubtensorStakingViewModel) {
        viewModel.state.observe { state ->
            renderState(state)
        }
        viewModel.errorEvents.observeEvent { message ->
            // Mirrors iOS `wireframe.showError(from:message:)` — surface the
            // failure without destroying any loaded content currently on
            // screen. Toast keeps it lightweight; dialog would interrupt.
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
        }
        viewModel.unstakeRequest.observeEvent { action ->
            when (action) {
                is SubtensorStakingViewModel.UnstakeAction.Empty -> android.widget.Toast.makeText(
                    requireContext(),
                    R.string.subtensor_unstake_no_positions,
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                is SubtensorStakingViewModel.UnstakeAction.Single -> viewModel.unstakeOptionPicked(action)
                is SubtensorStakingViewModel.UnstakeAction.Pick -> showUnstakePickerDialog(action)
            }
        }
    }

    private fun showUnstakePickerDialog(action: SubtensorStakingViewModel.UnstakeAction.Pick) {
        // Mirrors iOS `SubtensorStakingWireframe.showUnstake` action sheet:
        // a list of positions (validator identity or shortened hotkey) the
        // user picks one of before being routed onward to UnstakeSetup.
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.subtensor_unstake_pick_validator)
            .setItems(action.labels.toTypedArray()) { _, which ->
                viewModel.unstakeOptionPicked(action.options[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renderState(state: SubtensorStakingViewModel.UiState) {
        val loading = binder.subtensorLoading
        val empty = binder.subtensorEmptyState
        val content = binder.subtensorContent

        when (state) {
            SubtensorStakingViewModel.UiState.Loading -> {
                loading.isVisible = true
                empty.isVisible = false
                content.isVisible = false
            }
            SubtensorStakingViewModel.UiState.Empty -> {
                loading.isVisible = false
                empty.isVisible = true
                content.isVisible = false
            }
            is SubtensorStakingViewModel.UiState.Loaded -> {
                loading.isVisible = false
                empty.isVisible = false
                content.isVisible = true
                bindLoaded(state)
            }
        }
    }

    private fun bindLoaded(state: SubtensorStakingViewModel.UiState.Loaded) {
        binder.subtensorTotalStakeAmount.text = state.totalAmountText

        val badge = state.netuidBadge
        binder.subtensorTotalStakeBadge.apply {
            text = badge
            isVisible = badge != null
        }

        binder.subtensorMinStake.text = state.minStakeText
        binder.subtensorUnstakingPeriod.text = state.unstakingPeriodText

        binder.subtensorValidatorCard.isVisible = state.validators.isNotEmpty()
        binder.subtensorValidatorTitle.text = state.validatorsTitle
        renderValidatorRows(state.validators)
    }

    private fun renderValidatorRows(rows: List<SubtensorStakingViewModel.ValidatorRow>) {
        val container = binder.subtensorValidatorRows
        container.removeAllViews()
        rows.forEachIndexed { index, row ->
            val view = layoutInflater.inflate(R.layout.item_subtensor_validator_row, container, false)
            view.findViewById<ImageView>(R.id.subtensorValidatorRowIcon).apply {
                setImageDrawable(row.identicon)
                isVisible = row.identicon != null
            }
            view.findViewById<TextView>(R.id.subtensorValidatorRowName).text =
                row.identityName ?: row.hotkeyShort
            view.findViewById<TextView>(R.id.subtensorValidatorRowHotkey).apply {
                // The whole screen is netuid-scoped, so each row's subtitle
                // shows only the truncated hotkey (matches iOS
                // `SubtensorValidatorListView.swift:150-151`). The netuid
                // appears once on the stake-total card via `netuidBadge`.
                text = row.hotkeyShort
                isVisible = row.identityName != null
            }
            view.findViewById<TextView>(R.id.subtensorValidatorRowAmount).text = row.amountText
            view.findViewById<View>(R.id.subtensorValidatorRowDivider).isVisible = index < rows.size - 1
            (container as LinearLayout).addView(view)
        }
    }
}
