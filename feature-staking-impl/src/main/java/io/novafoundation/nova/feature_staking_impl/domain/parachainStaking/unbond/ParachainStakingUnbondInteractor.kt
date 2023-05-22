package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.scheduleBondLess
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.scheduleRevokeDelegation
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

interface ParachainStakingUnbondInteractor {

    suspend fun estimateFee(amount: BigInteger, collatorId: AccountId): BigInteger

    suspend fun unbond(amount: BigInteger, collator: AccountId): Result<*>

    suspend fun canUnbond(fromCollator: AccountId, delegatorState: DelegatorState): Boolean

    suspend fun getSelectedCollators(delegatorState: DelegatorState): List<UnbondingCollator>
}

class RealParachainStakingUnbondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val delegatorStateRepository: DelegatorStateRepository,
    private val selectedAssetSharedState: AnySelectedAssetOptionSharedState,
    private val collatorsUseCase: CollatorsUseCase,
) : ParachainStakingUnbondInteractor {

    override suspend fun estimateFee(amount: BigInteger, collatorId: AccountId): BigInteger {
        val chain = selectedAssetSharedState.chain()

        return extrinsicService.estimateFee(chain) {
            unbond(amount, collatorId)
        }
    }

    override suspend fun unbond(amount: BigInteger, collator: AccountId): Result<*> = withContext(Dispatchers.IO) {
        val chain = selectedAssetSharedState.chain()

        extrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(chain) {
            unbond(amount, collator)
        }
    }

    override suspend fun canUnbond(fromCollator: AccountId, delegatorState: DelegatorState): Boolean = withContext(Dispatchers.IO) {
        when (delegatorState) {
            is DelegatorState.Delegator -> {
                val scheduledDelegationRequest = delegatorStateRepository.scheduledDelegationRequest(delegatorState, fromCollator)

                scheduledDelegationRequest == null // can unbond only if there is no scheduled request already
            }
            is DelegatorState.None -> false
        }
    }

    override suspend fun getSelectedCollators(delegatorState: DelegatorState): List<UnbondingCollator> = withContext(Dispatchers.Default) {
        when (delegatorState) {
            is DelegatorState.Delegator -> {
                val collators = collatorsUseCase.getSelectedCollators(delegatorState)
                val unbondings = delegatorStateRepository.scheduledDelegationRequests(delegatorState)

                collators.map { selectedCollator ->
                    UnbondingCollator(
                        selectedCollator = selectedCollator,
                        hasPendingUnbonding = selectedCollator.collator.accountIdHex in unbondings
                    )
                }
            }

            is DelegatorState.None -> emptyList()
        }
    }

    private suspend fun ExtrinsicBuilder.unbond(amount: BigInteger, collatorId: AccountId) {
        val delegatorState = delegatorStateUseCase.currentDelegatorState()
        val delegationAmount = delegatorState.delegationAmountTo(collatorId).orZero()

        if (amount >= delegationAmount) {
            scheduleRevokeDelegation(collatorId)
        } else {
            scheduleBondLess(collatorId, amount)
        }
    }
}
