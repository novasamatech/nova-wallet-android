package io.novafoundation.nova.feature_staking_impl.presentation.staking.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.SpannableFormatter
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.formatAsSpannable
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.setEndSpan
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingLandingInfoUpdateSystemFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.ParticipationInGovernance
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingCompoundData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model.StakingConditionRVItem
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class StartStakingInfoModel(
    val title: CharSequence,
    val conditions: List<StakingConditionRVItem>,
    val moreInfo: CharSequence
)

class StartStakingLandingViewModel(
    private val stakingRouter: StakingRouter,
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
    private val updateSystemFactory: StakingLandingInfoUpdateSystemFactory,
    private val startStakingInteractorFactory: StartStakingInteractorFactory,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(), Browserable {

    private val startStakingInteractor = stakingSharedState.assetWithChain.map {
        startStakingInteractorFactory.create(it.chain, it.asset, coroutineScope = this)
    }.shareInBackground()

    private val startStakingInfo = startStakingInteractor.flatMapLatest {
        it.observeStartStakingInfo(stakingSharedState.chain(), stakingSharedState.chainAsset())
    }.withLoadingShared()
        .shareInBackground()

    val modelFlow = startStakingInfo
        .mapLoading {
            StartStakingInfoModel(
                title = createTitle(it.asset.token.configuration, it.maxEarningRate),
                conditions = createConditions(it),
                moreInfo = createMoreInfoText()
            )
        }.shareInBackground()

    val availableBalanceTextFlow = startStakingInfo.mapLoading {
        val amountModel = mapAmountToAmountModel(it.availableBalance, it.asset.token)
        resourceManager.getString(R.string.start_staking_fragment_available_balance, amountModel.token, amountModel.fiat!!)
    }.shareInBackground()

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        updateSystemFactory.create(stakingSharedState.assetWithChain)
            .start()
            .launchIn(this)
    }

    fun back() {
        stakingRouter.back()
    }

    fun termsOfUseClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    private fun createTitle(chainAsset: Chain.Asset, earning: BigDecimal): CharSequence {
        val color = resourceManager.getColor(R.color.text_positive)
        val apy = resourceManager.getString(
            R.string.start_staking_fragment_title_APY,
            earning.formatFractionAsPercentage()
        ).toSpannable(colorSpan(color))

        return SpannableFormatter.format(
            resourceManager.getString(R.string.start_staking_fragment_title),
            apy,
            chainAsset.symbol
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

    private fun createConditions(data: StartStakingCompoundData): List<StakingConditionRVItem> {
        return listOfNotNull(
            createTestNetworkCondition(data.chain),
            createMinStakeCondition(data.asset, data.minStake, data.eraInfo.remainingEraTime),
            createUnstakeCondition(data.eraInfo.unstakeTime),
            createRewardsFrequencyCondition(data.eraInfo.eraDuration, data.automaticPayoutMinAmount, data.asset, data.payoutTypes),
            createGovernanceParticipatingCondition(data.asset, data.participationInGovernance),
            createStakeMonitoring()
        )
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

    private fun createMinStakeCondition(
        asset: Asset,
        minStakeAmount: BigInteger,
        eraDuration: Duration
    ): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val minStake = minStakeAmount.formatPlanks(asset.token.configuration)
            .toSpannable(colorSpan(color))
        val time = resourceManager.getString(
            R.string.start_staking_fragment_min_stake_condition_duration,
            resourceManager.formatDuration(eraDuration, false)
        ).toSpannable(colorSpan(color))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_stake_anytime,
            text = resourceManager.getString(R.string.start_staking_fragment_min_stake_condition).formatAsSpannable(minStake, time),
        )
    }

    private fun createUnstakeCondition(unstakeDuration: Duration): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val time = resourceManager.getString(
            R.string.start_staking_fragment_unstake_condition_duration,
            resourceManager.formatDuration(unstakeDuration, false)
        ).toSpannable(colorSpan(color))
        return StakingConditionRVItem(
            iconId = R.drawable.ic_unstake_anytime,
            text = resourceManager.getString(R.string.start_staking_fragment_unstake_condition).formatAsSpannable(time),
        )
    }

    private fun createRewardsFrequencyCondition(
        eraDuration: Duration,
        automaticPayoutMinAmount: BigInteger?,
        asset: Asset,
        payoutTypes: List<PayoutType>
    ): StakingConditionRVItem {
        val color = resourceManager.getColor(R.color.text_positive)
        val time = resourceManager.formatDuration(eraDuration, false).toSpannable(colorSpan(color))

        val text = when {
            payoutTypes.containsOnly(PayoutType.Automatic.Restake) -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_restake_only).formatAsSpannable(time)
            }
            payoutTypes.containsOnly(PayoutType.Automatic.Payout) -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_payout_only).formatAsSpannable(time)
            }
            payoutTypes.containsOnly(PayoutType.Manual) -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_manual).formatAsSpannable(time)
            }
            payoutTypes.containsManualAndAutomatic() -> {
                val automaticPayoutFormattedAmount = automaticPayoutMinAmount?.formatPlanks(asset.token.configuration) ?: ""
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_automatic_and_manual)
                    .formatAsSpannable(time, automaticPayoutFormattedAmount)
            }
            else -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_fallback)
                    .formatAsSpannable(time)
            }
        }

        return StakingConditionRVItem(
            iconId = R.drawable.ic_rewards,
            text = text,
        )
    }

    private fun createGovernanceParticipatingCondition(
        asset: Asset,
        participationInGovernance: ParticipationInGovernance
    ): StakingConditionRVItem? {
        if (participationInGovernance !is ParticipationInGovernance.Participate) return null

        val color = resourceManager.getColor(R.color.text_positive)

        val text = if (participationInGovernance.minAmount != null) {
            val minAmount = participationInGovernance.minAmount.formatPlanks(asset.token.configuration)
            val participation = resourceManager.getString(R.string.start_staking_fragment_governance_participation_with_min_amount_accent)
                .toSpannable(colorSpan(color))
            resourceManager.getString(R.string.start_staking_fragment_governance_participation_with_min_amount).formatAsSpannable(minAmount, participation)
        } else {
            val participation = resourceManager.getString(R.string.start_staking_fragment_governance_participation_no_conditions_accent)
                .toSpannable(colorSpan(color))
            resourceManager.getString(R.string.start_staking_fragment_governance_participation_no_conditions).formatAsSpannable(participation)
        }

        return StakingConditionRVItem(
            iconId = R.drawable.ic_participate_in_governance,
            text = text,
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
        openBrowserEvent.value = Event("TODO")
    }

    private fun List<PayoutType>.containsOnly(type: PayoutType): Boolean {
        return contains(type) && size == 1
    }

    private fun List<PayoutType>.containsManualAndAutomatic(): Boolean {
        return contains(PayoutType.Manual) && any { it is PayoutType.Automatic } && size == 2
    }
}
