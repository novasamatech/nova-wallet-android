package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class YieldBoostParameters(
    val yearlyReturns: PeriodReturns,
    val periodInDays: Int
)

class YieldBoostTask(
    val id: String,
    val collator: AccountId,
    val accountMinimum: Balance,
    val frequencyInBlocks: BlockNumber,
    val frequency: Duration,
)

fun YieldBoostTask.frequencyInDays() = frequency.toInt(DurationUnit.DAYS).coerceAtLeast(1)

interface YieldBoostInteractor {

    suspend fun optimalYieldBoostParameters(delegatorState: DelegatorState, collatorId: AccountId): YieldBoostParameters

    fun activeYieldBoostTasks(delegatorState: DelegatorState.Delegator): Flow<List<YieldBoostTask>>
}

class RealYieldBoostInteractor(
    private val yieldBoostRepository: TuringAutomationTasksRepository,
    private val chainStateRepository: ChainStateRepository,
) : YieldBoostInteractor {
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
            val blockTime = chainStateRepository.predictedBlockTime(delegatorState.chain.id)

           tasks.map {
               YieldBoostTask(
                   id = it.id,
                   collator = it.collator,
                   accountMinimum = it.accountMinimum,
                   frequencyInBlocks = it.frequency,
                   frequency = (it.frequency * blockTime).toLong().milliseconds
               )
           }
        }
    }
}
