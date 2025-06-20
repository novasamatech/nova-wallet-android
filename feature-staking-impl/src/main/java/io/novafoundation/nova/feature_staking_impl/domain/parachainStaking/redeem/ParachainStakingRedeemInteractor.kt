package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.activeBonded
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.redeemableIn
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.executeDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

interface ParachainStakingRedeemInteractor {

    suspend fun estimateFee(delegatorState: DelegatorState): Fee

    suspend fun redeemableAmount(delegatorState: DelegatorState): BigInteger

    suspend fun redeem(delegatorState: DelegatorState): Result<Pair<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>, RedeemConsequences>>
}

class RealParachainStakingRedeemInteractor(
    private val extrinsicService: ExtrinsicService,
    private val currentRoundRepository: CurrentRoundRepository,
    private val delegatorStateRepository: DelegatorStateRepository,
) : ParachainStakingRedeemInteractor {

    override suspend fun estimateFee(delegatorState: DelegatorState): Fee = withContext(Dispatchers.Default) {
        extrinsicService.estimateFee(delegatorState.chain, TransactionOrigin.SelectedWallet) {
            redeem(delegatorState)
        }
    }

    override suspend fun redeemableAmount(delegatorState: DelegatorState): BigInteger {
        val redeemableUnbondings = getRedeemableUnbondings(delegatorState)

        return redeemableUnbondings.values.sumByBigInteger { it.action.amount }
    }

    override suspend fun redeem(delegatorState: DelegatorState) = withContext(Dispatchers.Default) {
        extrinsicService.submitAndWatchExtrinsic(delegatorState.chain, TransactionOrigin.SelectedWallet) {
            redeem(delegatorState)
        }
            .awaitInBlock()
            .map {
                it to RedeemConsequences(willKillStash = delegatorState.activeBonded.isZero)
            }
    }

    private suspend fun ExtrinsicBuilder.redeem(delegatorState: DelegatorState) {
        val redeemableUnbondings = getRedeemableUnbondings(delegatorState)

        redeemableUnbondings.forEach { (collatorIdHex, redeemableRequest) ->
            executeDelegationRequest(
                collatorId = collatorIdHex.fromHex(),
                delegator = redeemableRequest.delegator
            )
        }
    }

    private suspend fun getRedeemableUnbondings(delegatorState: DelegatorState): AccountIdMap<ScheduledDelegationRequest> {
        return when (delegatorState) {
            is DelegatorState.Delegator -> {
                val currentRound = currentRoundRepository.currentRoundInfo(delegatorState.chain.id).current
                val scheduledRequests = delegatorStateRepository.scheduledDelegationRequests(delegatorState)

                scheduledRequests.filterValues { request -> request.redeemableIn(currentRound) }
            }

            is DelegatorState.None -> emptyMap()
        }
    }
}
