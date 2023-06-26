package io.novafoundation.nova.feature_staking_impl.presentation.staking.landing

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.SpannableFormatter
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatAsSpannable
import io.novafoundation.nova.common.utils.setEndSpan
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.landing.model.StakingConditionRVItem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StartStakingLandingViewModel(
    private val stakingRouter: StakingRouter,
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val titleFlow: Flow<CharSequence> = stakingSharedState.selectedChainFlow()
        .map { chain ->
            createTitle()
        }.shareInBackground()

    val stakingConditionsUIFlow: Flow<List<StakingConditionRVItem>> = flowOf {
        createConditions(stakingSharedState.chain())
    }

    val moreInfoTextFlow: Flow<CharSequence> = flowOf {
        createMoreInfoText()
    }

    fun backClicked() {
        stakingRouter.back()
    }

    fun termsOfUseClicked() {
        // TODO
    }

    private fun createTitle(): CharSequence {
        val color = resourceManager.getColor(R.color.text_positive)
        val apy = resourceManager.getString(
            R.string.start_staking_fragment_title_APY,
            "22.84%"
        ).toSpannable(colorSpan(color))

        return SpannableFormatter.format(
            resourceManager.getString(R.string.start_staking_fragment_title),
            apy,
            "DOT"
        )
    }

    private fun createMoreInfoText(): CharSequence {
        val iconColor = resourceManager.getColor(R.color.chip_icon)
        val clickableTextColor = resourceManager.getColor(R.color.text_secondary)
        val chevronSize = resourceManager.measureInPx(20)
        val chevronRight = resourceManager.getDrawable(R.drawable.ic_chevron_right).apply {
            setBounds(0, 0, chevronSize, chevronSize)
            setTint(iconColor)
        }
        val clickablePart = resourceManager.getString(R.string.start_staking_fragment_more_info_clicable_part)
            .toSpannable(colorSpan(clickableTextColor))
            .setFullSpan(clickableSpan { novaWikiClicked() })
            .setEndSpan(drawableSpan(chevronRight))

        return SpannableFormatter.format(
            resourceManager.getString(R.string.start_staking_fragment_more_info),
            clickablePart
        )
    }

    private fun createConditions(chain: Chain): List<StakingConditionRVItem> {
        return buildList {
            this += createTestNetworkCondition(chain)
            this += createMinStakeCondition()
            this += createUnstakeCondition()
            this += createRewardsFrequencyCondition()
            this += createGovernanceParticipatingCondition()
            this += createStakeMonitoring()
        }.filterNotNull()
    }

    private fun createTestNetworkCondition(chain: Chain): StakingConditionRVItem? {
        if (!chain.isTestNet) {
            return null
        }

        val color = resourceManager.getColor(R.color.text_positive)
        val chainName = chain.name.toSpannable(colorSpan(color))
        val testNetwork = resourceManager.getString(R.string.start_staking_fragment_test_network_condition_test_network)
            .toSpannable(colorSpan(color))
        val noTokenValue = resourceManager.getString(R.string.start_staking_fragment_test_network_condition_no_token)
            .toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_test_network,
            text = resourceManager.getString(R.string.start_staking_fragment_test_network_condition).formatAsSpannable(chainName, testNetwork, noTokenValue),
        )
    }

    private fun createMinStakeCondition(): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val minStake = "1 DOT".toSpannable(colorSpan(color))
        val time = "in 4 hours and 34 minutes".toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_stake_anytime,
            text = resourceManager.getString(R.string.start_staking_fragment_min_stake_condition).formatAsSpannable(minStake, time),
        )
    }

    private fun createUnstakeCondition(): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val time = "after 1 day and 4 hours".toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_unstake_anytime,
            text = resourceManager.getString(R.string.start_staking_fragment_unstake_condition).formatAsSpannable(time),
        )
    }

    private fun createRewardsFrequencyCondition(): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val time = "6 hours".toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_rewards,
            text = resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition).formatAsSpannable(time),
        )
    }

    private fun createGovernanceParticipatingCondition(): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val participation = resourceManager.getString(R.string.start_staking_fragment_governance_participation_participate).toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_participate_in_governance,
            text = resourceManager.getString(R.string.start_staking_fragment_governance_participation).formatAsSpannable(participation),
        )
    }

    private fun createStakeMonitoring(): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val monitorStaking = resourceManager.getString(R.string.start_staking_fragment_stake_monitoring_monitor_stake).toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_monitor_your_stake,
            text = resourceManager.getString(R.string.start_staking_fragment_stake_monitoring).formatAsSpannable(monitorStaking),
        )
    }

    private fun novaWikiClicked() {
        // TODO
    }
}
