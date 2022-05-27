package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.scheduleBondLess
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.scheduleRevokeDelegation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

interface ParachainStakingUnbondInteractor {

    suspend fun estimateFee(amount: BigInteger, collatorId: AccountId): BigInteger

    suspend fun unbond(amount: BigInteger, collator: AccountId): Result<*>
}

class RealParachainStakingUnbondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val selectedAssetSharedState: SingleAssetSharedState,
): ParachainStakingUnbondInteractor {


    override suspend fun estimateFee(amount: BigInteger, collatorId: AccountId): BigInteger {
        val chain = selectedAssetSharedState.chain()

        return extrinsicService.estimateFee(chain) {
            unbond(amount, collatorId)
        }
    }

    override suspend fun unbond(amount: BigInteger, collator: AccountId): Result<*> = withContext(Dispatchers.IO) {
        val chain = selectedAssetSharedState.chain()

        extrinsicService.submitExtrinsic(chain) {
            unbond(amount, collator)
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
