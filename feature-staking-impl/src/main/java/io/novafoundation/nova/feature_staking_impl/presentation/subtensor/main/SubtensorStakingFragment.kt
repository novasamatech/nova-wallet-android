package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatAsSpannable
import io.novafoundation.nova.common.utils.toSpannable
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
        binder.subtensorLandingStartButton.setOnClickListener { viewModel.stakeMore() }
        binder.subtensorLandingMoreInfo.setOnClickListener { viewModel.novaWikiClicked() }
        // Tint the Start CTA gold — matches AZERO/etc. landings, which
        // colour the button using the chain's theme accent. Bittensor's
        // accent in chains_dev is the warning gold (#EBC50A).
        binder.subtensorLandingStartButton.setButtonColor(goldAccent())
        // Static bullet copy — only the highlighted ranges depend on
        // dynamic data (max APY, min stake, etc), bound below in subscribe().
        binder.subtensorLandingBulletStake.text = buildBulletStake()
        binder.subtensorLandingBulletRewards.text = buildBulletRewards()
        binder.subtensorLandingBulletMonitor.text = buildBulletMonitor()
        binder.subtensorLandingMoreInfo.text = buildMoreInfoText()
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
        viewModel.maxApy.observe { maxApy ->
            binder.subtensorLandingTitle.text = buildTitle(maxApy)
        }
        viewModel.availableBalance.observe { balance ->
            binder.subtensorLandingAvailableBalance.text = balance?.let {
                getString(R.string.subtensor_landing_available_balance, it)
            }
        }
        viewModel.openBrowserEvent.observeEvent { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
        // The bottom Start-staking + balance container is paired with the
        // empty/landing state — hidden in every other state so the regular
        // detail content takes the full screen.
        val landingBottom = binder.subtensorLandingBottomContainer
        val toolbar = binder.subtensorStakingToolbar
        val root = binder.subtensorRoot

        when (state) {
            SubtensorStakingViewModel.UiState.Loading -> {
                loading.isVisible = true
                empty.isVisible = false
                content.isVisible = false
                landingBottom.isVisible = false
                toolbar.setTitle("")
                toolbar.setHomeButtonIcon(R.drawable.ic_close)
                // Default Nova gradient while we don't know yet whether
                // this account has a stake or lands on the gold landing.
                root.setBackgroundResource(R.drawable.drawable_background_image)
            }
            SubtensorStakingViewModel.UiState.Empty -> {
                loading.isVisible = false
                empty.isVisible = true
                content.isVisible = false
                landingBottom.isVisible = true
                // Modal-feeling landing matches AZERO etc.: no title, X close
                // icon, solid black background. Distinct from the populated
                // detail state which gets a title + back arrow + gradient.
                toolbar.setTitle("")
                toolbar.setHomeButtonIcon(R.drawable.ic_close)
                root.setBackgroundResource(R.color.secondary_screen_background)
            }
            is SubtensorStakingViewModel.UiState.Loaded -> {
                loading.isVisible = false
                empty.isVisible = false
                content.isVisible = true
                landingBottom.isVisible = false
                toolbar.setTitle(getString(R.string.subtensor_staking_title))
                toolbar.setHomeButtonIcon(R.drawable.ic_arrow_back)
                // Populated detail uses the standard Nova blue gradient,
                // matching every other staking detail screen.
                root.setBackgroundResource(R.drawable.drawable_background_image)
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

    /** Theme accent colour for the gold-landing highlights. Mirrors iOS's
     *  `R.color.colorTextWarning` / Bittensor brand gold. Falls back to
     *  Nova's positive-amount colour if no chain theme is configured. */
    private fun goldAccent(): Int = requireContext().getColor(R.color.text_warning)

    private fun buildTitle(maxApy: String): CharSequence {
        val accent = maxApy.toSpannable(colorSpan(goldAccent()))
        return getString(R.string.subtensor_landing_title).formatAsSpannable(accent)
    }

    private fun buildBulletStake(): CharSequence {
        val minStake = getString(R.string.subtensor_landing_min_stake_value).toSpannable(colorSpan(goldAccent()))
        val firstReward = getString(R.string.subtensor_landing_first_reward_time).toSpannable(colorSpan(goldAccent()))
        return getString(R.string.subtensor_landing_min_stake_condition).formatAsSpannable(minStake, firstReward)
    }

    private fun buildBulletRewards(): CharSequence {
        val frequency = getString(R.string.subtensor_landing_rewards_frequency_value)
            .toSpannable(colorSpan(goldAccent()))
        return getString(R.string.subtensor_landing_rewards_frequency_condition).formatAsSpannable(frequency)
    }

    private fun buildBulletMonitor(): CharSequence {
        val monitor = getString(R.string.subtensor_landing_monitor_accent)
            .toSpannable(colorSpan(goldAccent()))
        return getString(R.string.subtensor_landing_monitor_condition).formatAsSpannable(monitor)
    }

    private fun buildMoreInfoText(): CharSequence {
        val link = getString(R.string.subtensor_landing_wiki_link)
            .toSpannable(colorSpan(requireContext().getColor(R.color.button_text_accent)))
        return getString(R.string.subtensor_landing_more_info).formatAsSpannable(link)
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
