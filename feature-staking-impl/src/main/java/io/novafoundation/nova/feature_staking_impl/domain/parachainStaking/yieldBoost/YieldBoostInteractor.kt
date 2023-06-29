package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTask
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.TimestampRepository
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class YieldBoostParameters(
    val yearlyReturns: PeriodReturns,
    val periodInDays: Int
)

interface YieldBoostInteractor {

    suspend fun calculateFee(
        configuration: YieldBoostConfiguration,
        activeTasks: List<YieldBoostTask>,
    ): BigInteger

    suspend fun setYieldBoost(
        configuration: YieldBoostConfiguration,
        activeTasks: List<YieldBoostTask>
    ): Result<ExtrinsicStatus.InBlock>

    suspend fun optimalYieldBoostParameters(delegatorState: DelegatorState, collatorId: AccountId): YieldBoostParameters

    fun activeYieldBoostTasks(delegatorState: DelegatorState.Delegator): Flow<List<YieldBoostTask>>
}

class RealYieldBoostInteractor(
    private val yieldBoostRepository: TuringAutomationTasksRepository,
    private val extrinsicService: ExtrinsicService,
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val timestampRepository: TimestampRepository,
) : YieldBoostInteractor {

    override suspend fun calculateFee(
        configuration: YieldBoostConfiguration,
        activeTasks: List<YieldBoostTask>
    ): BigInteger {
        val chain = singleAssetSharedState.chain()

        return extrinsicService.estimateFee(chain) {
            setYieldBoost(chain, activeTasks, configuration)
        }
    }

    override suspend fun setYieldBoost(configuration: YieldBoostConfiguration, activeTasks: List<YieldBoostTask>): Result<ExtrinsicStatus.InBlock> {
        val chain = singleAssetSharedState.chain()

        return extrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(chain) {
            setYieldBoost(chain, activeTasks, configuration)
        }
    }

    override suspend fun optimalYieldBoostParameters(delegatorState: DelegatorState, collatorId: AccountId): YieldBoostParameters {
        val amountInPlanks = delegatorState.delegationAmountTo(collatorId).orZero()
        val amount = delegatorState.chainAsset.amountFromPlanks(amountInPlanks)

        val collatorAddress = delegatorState.chain.addressOf(collatorId)

        val request = OptimalAutomationRequest(collatorAddress, amountInPlanks)
        val optimalAutomationResponse = yieldBoostRepository.calculateOptimalAutomation(delegatorState.chain.id, request)

        val apy = optimalAutomationResponse.apy.toBigDecimal()

        return YieldBoostParameters(
            yearlyReturns = PeriodReturns(
                gainFraction = apy,
                gainAmount = apy * amount
            ),
            periodInDays = optimalAutomationResponse.period
        )
    }

    override fun activeYieldBoostTasks(delegatorState: DelegatorState.Delegator): Flow<List<YieldBoostTask>> {
        return yieldBoostRepository.automationTasksFlow(delegatorState.chain.id, delegatorState.accountId).map { tasks ->
            tasks.map {
                YieldBoostTask(
                    id = it.id,
                    collator = it.collator,
                    accountMinimum = it.accountMinimum,
                    schedule = mapYieldBoostTaskSchedule(it.schedule)
                )
            }
        }
    }

    private suspend fun ExtrinsicBuilder.setYieldBoost(
        chain: Chain,
        activeTasks: List<YieldBoostTask>,
        configuration: YieldBoostConfiguration
    ) {
        val collatorId = configuration.collatorIdHex.fromHex()
        val activeCollatorTask = activeTasks.findByCollator(collatorId)

        when (configuration) {
            is YieldBoostConfiguration.Off -> {
                activeCollatorTask?.let {
                    stopAutoCompounding(it)
                }
            }
            is YieldBoostConfiguration.On -> {
                if (activeCollatorTask != null) {
                    // updating existing yield-boost - cancel only modified collator task
                    stopAutoCompounding(activeCollatorTask)
                } else {
                    // setting up new yield boost - cancel every existing task
                    stopAllAutoCompounding(activeTasks)
                }

                startAutoCompounding(chain.id, configuration)
            }
        }
    }

    private suspend fun ExtrinsicBuilder.startAutoCompounding(chainId: ChainId, configuration: YieldBoostConfiguration.On) {
        val currentTimeStamp = timestampRepository.now(chainId).toLong().milliseconds
        val frequency = configuration.frequencyInDays.days

        val firstExecution = currentTimeStamp + frequency
        val firstExecutionInHours = firstExecution.toDouble(DurationUnit.HOURS)
        val firstExecutionRoundedToHours = firstExecutionInHours.roundToLong().hours.inWholeSeconds.toBigInteger()

        call(
            moduleName = Modules.AUTOMATION_TIME,
            callName = "schedule_auto_compound_delegated_stake_task",
            arguments = mapOf(
                "execution_time" to firstExecutionRoundedToHours,
                "frequency" to frequency.inWholeSeconds.toBigInteger(),
                "collator_id" to configuration.collatorIdHex.fromHex(),
                "account_minimum" to configuration.threshold
            )
        )
    }

    private fun ExtrinsicBuilder.stopAllAutoCompounding(tasks: List<YieldBoostTask>) {
        tasks.forEach { task ->
            stopAutoCompounding(task)
        }
    }

    private fun ExtrinsicBuilder.stopAutoCompounding(task: YieldBoostTask) {
        call(
            moduleName = Modules.AUTOMATION_TIME,
            callName = "cancel_task",
            arguments = mapOf(
                "task_id" to task.id.fromHex()
            )
        )
    }

    private fun mapYieldBoostTaskSchedule(task: TuringAutomationTask.Schedule): YieldBoostTask.Schedule {
        return when (task) {
            TuringAutomationTask.Schedule.Unknown -> YieldBoostTask.Schedule.Unknown
            is TuringAutomationTask.Schedule.Recurring -> YieldBoostTask.Schedule.Recurring(
                frequency = task.frequency.toLong().seconds
            )
        }
    }
}
