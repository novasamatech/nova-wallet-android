package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.calls.cancelDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

interface ParachainStakingRebondInteractor {

    suspend fun estimateFee(collatorId: AccountId): BigInteger

    suspend fun rebondAmount(
        delegatorState: DelegatorState,
        collatorId: AccountId
    ): BigInteger

    suspend fun rebond(collatorId: AccountId): Result<*>
}

class RealParachainStakingRebondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val delegatorStateRepository: DelegatorStateRepository,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
) : ParachainStakingRebondInteractor {

    override suspend fun estimateFee(collatorId: AccountId): BigInteger = withContext(Dispatchers.IO) {
        extrinsicService.estimateFee(selectedAssetState.chain()) {
            cancelDelegationRequest(collatorId)
        }
    }

    override suspend fun rebondAmount(delegatorState: DelegatorState, collatorId: AccountId): BigInteger = withContext(Dispatchers.IO) {
        when (delegatorState) {
            is DelegatorState.Delegator -> {
                val request = delegatorStateRepository.scheduledDelegationRequest(delegatorState, collatorId)

                request?.action?.amount.orZero()
            }
            is DelegatorState.None -> BigInteger.ZERO
        }
    }

    override suspend fun rebond(collatorId: AccountId): Result<*> = withContext(Dispatchers.IO) {
        extrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(selectedAssetState.chain()) {
            cancelDelegationRequest(collatorId)
        }
    }
}
