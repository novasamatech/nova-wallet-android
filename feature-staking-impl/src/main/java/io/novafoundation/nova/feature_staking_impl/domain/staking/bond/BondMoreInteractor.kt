package io.novafoundation.nova.feature_staking_impl.domain.staking.bond

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.stashTransactionOrigin
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.bondMore
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class BondMoreInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
) {

    suspend fun estimateFee(amount: BigInteger, stakingState: StakingState.Stash): Fee {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()

            extrinsicService.estimateFee(chain, stakingState.stashTransactionOrigin()) {
                bondMore(amount)
            }
        }
    }

    suspend fun bondMore(stashAddress: String, amount: BigInteger): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()
            val accountId = chain.accountIdOf(stashAddress)

            extrinsicService.submitExtrinsic(chain, accountId.intoOrigin()) {
                bondMore(amount)
            }
        }
    }
}
