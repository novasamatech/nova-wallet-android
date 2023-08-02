package io.novafoundation.nova.feature_staking_impl.presentation.staking.start

import android.graphics.Color
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
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatAsSpannable
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.setEndSpan
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingLandingInfoUpdateSystemFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.ParticipationInGovernance
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.Payouts
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingCompoundData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model.StakingConditionRVItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class StartStakingInfoModel(
    val title: CharSequence,
    val conditions: List<StakingConditionRVItem>,
    val moreInfo: CharSequence,
    val buttonColor: Int
)

class StartStakingLandingViewModel(
    private val stakingRouter: StakingRouter,
    private val resourceManager: ResourceManager,
    private val updateSystemFactory: StakingLandingInfoUpdateSystemFactory,
    private val startStakingInteractorFactory: StartStakingInteractorFactory,
    private val appLinksProvider: AppLinksProvider,
    private val startStakingLandingPayload: StartStakingLandingPayload
) : BaseViewModel(), Browserable {

    private val startStakingInteractor = flowOf {
        startStakingInteractorFactory.create(
            startStakingLandingPayload.chainId,
            startStakingLandingPayload.assetId,
            startStakingLandingPayload.stakingTypes,
            coroutineScope = this
        )
    }.shareInBackground()

    private val startStakingInfo = startStakingInteractor.flatMapLatest { interactor ->
        interactor.observeStartStakingInfo()
    }.withLoadingShared()
        .shareInBackground()

    val modelFlow = startStakingInfo
        .mapLoading {
            val themeColor = getThemeColor(it.chain)
            StartStakingInfoModel(
                title = createTitle(it.asset.token.configuration, it.maxEarningRate, themeColor),
                conditions = createConditions(it, themeColor),
                moreInfo = createMoreInfoText(it.chain),
                buttonColor = themeColor
            )
        }.shareInBackground()

    val availableBalance = startStakingInteractor.flatMapLatest { interactor ->
        interactor.observeAvailableBalance()
    }.shareInBackground()

    val availableBalanceTextFlow = availableBalance.map { availableBalance ->
        val amountModel = mapAmountToAmountModel(availableBalance.availableBalance, availableBalance.asset.token)
        resourceManager.getString(R.string.start_staking_fragment_available_balance, amountModel.token, amountModel.fiat!!)
    }.shareInBackground()

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        updateSystemFactory.create(startStakingLandingPayload.chainId, startStakingLandingPayload.stakingTypes)
            .start()
            .launchIn(this)
    }

    fun back() {
        stakingRouter.back()
    }

    fun termsOfUseClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    private fun createTitle(chainAsset: Chain.Asset, earning: BigDecimal, themeColor: Int): CharSequence {
        val apy = resourceManager.getString(
            R.string.start_staking_fragment_title_APY,
            earning.formatFractionAsPercentage()
        ).toSpannable(colorSpan(themeColor))

        return SpannableFormatter.format(
            resourceManager.getString(R.string.start_staking_fragment_title),
            apy,
            chainAsset.symbol
        )
    }

    private fun createMoreInfoText(chain: Chain): CharSequence {
        val iconColor = resourceManager.getColor(R.color.chip_icon)
        val clickableTextColor = resourceManager.getColor(R.color.text_secondary)
        val chevronSize = resourceManager.measureInPx(20)
        val chevronRight = resourceManager.getDrawable(R.drawable.ic_chevron_right).apply {
            setBounds(0, 0, chevronSize, chevronSize)
            setTint(iconColor)
        }
        val clickablePart = resourceManager.getString(R.string.start_staking_fragment_more_info_clicable_part)
            .toSpannable(colorSpan(clickableTextColor))
            .setFullSpan(clickableSpan { novaWikiClicked(chain.additional?.stakingWiki) })
            .setEndSpan(drawableSpan(chevronRight))

        return SpannableFormatter.format(
            resourceManager.getString(R.string.start_staking_fragment_more_info),
            clickablePart
        )
    }

    private fun createConditions(data: StartStakingCompoundData, themeColor: Int): List<StakingConditionRVItem> {
        return listOfNotNull(
            createTestNetworkCondition(data.chain, themeColor),
            createMinStakeCondition(data.asset, data.minStake, data.eraInfo.remainingEraTime, themeColor),
            createUnstakeCondition(data.eraInfo.unstakeTime, themeColor),
            createRewardsFrequencyCondition(data.eraInfo.eraDuration, data.payouts, data.asset, themeColor),
            createGovernanceParticipatingCondition(data.asset, data.participationInGovernance, themeColor),
            createStakeMonitoring(themeColor)
        )
    }

    private fun createTestNetworkCondition(chain: Chain, themeColor: Int): StakingConditionRVItem? {
        if (!chain.isTestNet) {
            return null
        }
        val chainName = chain.name.toSpannable(colorSpan(themeColor))
        val testNetwork = resourceManager.getString(R.string.start_staking_fragment_test_network_condition_test_network)
            .toSpannable(colorSpan(themeColor))
        val noTokenValue = resourceManager.getString(R.string.start_staking_fragment_test_network_condition_no_token)
            .toSpannable(colorSpan(themeColor))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_test_network,
            text = resourceManager.getString(R.string.start_staking_fragment_test_network_condition).formatAsSpannable(chainName, testNetwork, noTokenValue),
        )
    }

    private fun createMinStakeCondition(
        asset: Asset,
        minStakeAmount: BigInteger,
        eraDuration: Duration,
        themeColor: Int
    ): StakingConditionRVItem {
        val minStake = minStakeAmount.formatPlanks(asset.token.configuration)
            .toSpannable(colorSpan(themeColor))
        val time = resourceManager.getString(
            R.string.start_staking_fragment_min_stake_condition_duration,
            resourceManager.formatDuration(eraDuration, false)
        ).toSpannable(colorSpan(themeColor))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_stake_anytime,
            text = resourceManager.getString(R.string.start_staking_fragment_min_stake_condition).formatAsSpannable(minStake, time),
        )
    }

    private fun createUnstakeCondition(
        unstakeDuration: Duration,
        themeColor: Int
    ): StakingConditionRVItem {
        val time = resourceManager.getString(
            R.string.start_staking_fragment_unstake_condition_duration,
            resourceManager.formatDuration(unstakeDuration, false)
        ).toSpannable(colorSpan(themeColor))
        return StakingConditionRVItem(
            iconId = R.drawable.ic_unstake_anytime,
            text = resourceManager.getString(R.string.start_staking_fragment_unstake_condition).formatAsSpannable(time),
        )
    }

    private fun createRewardsFrequencyCondition(
        eraDuration: Duration,
        payouts: Payouts,
        asset: Asset,
        themeColor: Int
    ): StakingConditionRVItem {
        val time = resourceManager.getString(
            R.string.start_staking_fragment_reward_frequency_condition_duration,
            resourceManager.formatDuration(eraDuration, false)
        ).toSpannable(colorSpan(themeColor))

        val payoutTypes = payouts.payoutTypes
        val text = when {
            isRestakeOnlyCase(payouts) -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_restake_only).formatAsSpannable(time)
            }
            isPayoutsOnlyCase(payouts) -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_payout_only).formatAsSpannable(time)
            }
            payoutTypes.containsOnly(PayoutType.Manual) -> {
                resourceManager.getString(R.string.start_staking_fragment_reward_frequency_condition_manual).formatAsSpannable(time)
            }
            payoutTypes.containsManualAndAutomatic() -> {
                val automaticPayoutFormattedAmount = payouts.automaticPayoutMinAmount?.formatPlanks(asset.token.configuration) ?: ""
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
        participationInGovernance: ParticipationInGovernance,
        themeColor: Int
    ): StakingConditionRVItem? {
        if (participationInGovernance !is ParticipationInGovernance.Participate) return null

        val text = if (participationInGovernance.minAmount != null) {
            val minAmount = participationInGovernance.minAmount.formatPlanks(asset.token.configuration)
            val participation = resourceManager.getString(R.string.start_staking_fragment_governance_participation_with_min_amount_accent)
                .toSpannable(colorSpan(themeColor))
            resourceManager.getString(R.string.start_staking_fragment_governance_participation_with_min_amount).formatAsSpannable(minAmount, participation)
        } else {
            val participation = resourceManager.getString(R.string.start_staking_fragment_governance_participation_no_conditions_accent)
                .toSpannable(colorSpan(themeColor))
            resourceManager.getString(R.string.start_staking_fragment_governance_participation_no_conditions).formatAsSpannable(participation)
        }

        return StakingConditionRVItem(
            iconId = R.drawable.ic_participate_in_governance,
            text = text,
        )
    }

    private fun createStakeMonitoring(themeColor: Int): StakingConditionRVItem {
        val monitorStaking = resourceManager.getString(R.string.start_staking_fragment_stake_monitoring_monitor_stake).toSpannable(colorSpan(themeColor))

        return StakingConditionRVItem(
            iconId = R.drawable.ic_monitor_your_stake,
            text = resourceManager.getString(R.string.start_staking_fragment_stake_monitoring).formatAsSpannable(monitorStaking),
        )
    }

    private fun novaWikiClicked(stakingWiki: String?) {
        openBrowserEvent.value = Event(stakingWiki ?: appLinksProvider.wikiBase)
    }

    private fun List<PayoutType>.containsOnly(type: PayoutType): Boolean {
        return contains(type) && size == 1
    }

    private fun List<PayoutType>.containsManualAndAutomatic(): Boolean {
        return contains(PayoutType.Manual) && any { it is PayoutType.Automatic } && size == 2
    }

    private fun isRestakeOnlyCase(payouts: Payouts): Boolean {
        return payouts.payoutTypes.containsOnly(PayoutType.Automatic.Restake) ||
            payouts.payoutTypes.contains(PayoutType.Automatic.Restake) && payouts.isAutomaticPayoutHasSmallestMinStake
    }

    private fun isPayoutsOnlyCase(payouts: Payouts): Boolean {
        return payouts.payoutTypes.containsOnly(PayoutType.Automatic.Payout) ||
            payouts.payoutTypes.contains(PayoutType.Automatic.Payout) && payouts.isAutomaticPayoutHasSmallestMinStake
    }

    private fun getThemeColor(chain: Chain): Int {
        return chain.additional?.themeColor?.let { Color.parseColor(it) }
            ?: resourceManager.getColor(R.color.text_positive)
    }
}
