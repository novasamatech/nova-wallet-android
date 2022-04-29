package io.novafoundation.nova.feature_staking_impl.domain.staking.rebond

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.rebond
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class RebondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val sharedStakingSate: StakingSharedState
) {

    suspend fun estimateFee(amount: BigInteger): BigInteger {
        return withContext(Dispatchers.IO) {
            val chain = sharedStakingSate.chain()

            extrinsicService.estimateFee(chain) {
                rebond(amount)
            }
        }
    }

    suspend fun rebond(stashState: StakingState.Stash, amount: BigInteger): Result<String> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stashState.chain, stashState.controllerId) {
                rebond(amount)
            }
        }
    }
}
