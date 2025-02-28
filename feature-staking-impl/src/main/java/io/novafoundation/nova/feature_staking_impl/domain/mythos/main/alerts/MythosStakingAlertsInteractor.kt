package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.alerts

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythReleaseRequest
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.totalRedeemable
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

interface MythosStakingAlertsInteractor {

    context(ComputationalScope)
    fun alertsFlow(delegatorState: MythosDelegatorState.Staked): Flow<List<MythosStakingAlert>>
}

@FeatureScope
class RealMythosStakingAlertsInteractor @Inject constructor(
    private val mythosSharedComputation: MythosSharedComputation,
    private val mythosUserStakeRepository: MythosUserStakeRepository,
    private val chainStateRepository: ChainStateRepository,
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository,
) : MythosStakingAlertsInteractor {

    context(ComputationalScope)
    override fun alertsFlow(delegatorState: MythosDelegatorState.Staked): Flow<List<MythosStakingAlert>> {
        return flowOfAll {
            val chain = stakingSharedState.chain()
            val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain).intoKey()

            combine(
                mythosSharedComputation.sessionValidatorsFlow(chain.id),
                mythosUserStakeRepository.releaseQueuesFlow(chain.id, accountId),
                chainStateRepository.currentBlockNumberFlow(chain.id)
            ) { sessionValidators, releaseQueues, currentBlockNumber ->
                val context = AlertCalculationContext(delegatorState, sessionValidators, currentBlockNumber, releaseQueues)
                alertProducers.mapNotNull { it.invoke(context) }
            }
        }
    }

    private fun changeCollatorsAlert(context: AlertCalculationContext): MythosStakingAlert.ChangeCollator? {
        val selectedCollators = context.delegationState.userStakeInfo.candidates
        val activeCollators = context.sessionValidators.toSet()

        val hasInactiveCollators = selectedCollators.any { it !in activeCollators }

        return MythosStakingAlert.ChangeCollator.takeIf { hasInactiveCollators }
    }

    private fun redeemAlert(context: AlertCalculationContext): MythosStakingAlert.RedeemTokens? {
        val totalRedeemAmount = context.releaseRequests.totalRedeemable(at = context.currentBlockNumber)

        return if (totalRedeemAmount.isPositive()) {
            MythosStakingAlert.RedeemTokens(totalRedeemAmount)
        } else {
            null
        }
    }

    private val alertProducers: List<AlertProducer> = listOf(
        ::changeCollatorsAlert,
        ::redeemAlert
    )
}

private typealias AlertProducer = (AlertCalculationContext) -> MythosStakingAlert?

private class AlertCalculationContext(
    val delegationState: MythosDelegatorState.Staked,
    val sessionValidators: SessionValidators,
    val currentBlockNumber: BlockNumber,
    val releaseRequests: List<MythReleaseRequest>
)
