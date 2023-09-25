package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.bondExtra
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NominationPoolsBondMoreInteractor {

    suspend fun estimateFee(bondMoreAmount: Balance): Balance

    suspend fun bondMore(bondMoreAmount: Balance): Result<String>
}

class RealNominationPoolsBondMoreInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
) : NominationPoolsBondMoreInteractor {

    override suspend fun estimateFee(bondMoreAmount: Balance): Balance {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingSharedState.chain()) {
                nominationPools.bondExtra(bondMoreAmount)
            }
        }
    }

    override suspend fun bondMore(bondMoreAmount: Balance): Result<String> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsicWithSelectedWallet(stakingSharedState.chain()) {
                nominationPools.bondExtra(bondMoreAmount)
            }
        }
    }
}
