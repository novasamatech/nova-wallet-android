package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.TimestampRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
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

    suspend fun optimalYieldBoostParameters(delegatorState: DelegatorState, collatorId: AccountId): YieldBoostParameters

    fun activeYieldBoostTasks(delegatorState: DelegatorState.Delegator): Flow<List<YieldBoostTask>>
}

class RealYieldBoostInteractor(
    private val yieldBoostRepository: TuringAutomationTasksRepository,
    private val extrinsicService: ExtrinsicService,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val timestampRepository: TimestampRepository,
) : YieldBoostInteractor {

    override suspend fun calculateFee(
        configuration: YieldBoostConfiguration,
        activeTasks: List<YieldBoostTask>
    ): BigInteger {
        val chain = singleAssetSharedState.chain()
        val collatorId = configuration.collatorIdHex.fromHex()
        val activeCollatorTask = activeTasks.findByCollator(collatorId)

        return extrinsicService.estimateFee(chain) {
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
                    frequency = it.frequencyInSeconds.toLong().seconds
                )
            }
        }
    }

    private suspend fun ExtrinsicBuilder.startAutoCompounding(chainId: ChainId, configuration: YieldBoostConfiguration.On) {
        val currentTimeStamp = timestampRepository.now(chainId).toLong().seconds
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
}
