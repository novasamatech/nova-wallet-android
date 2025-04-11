package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.NominationPoolBondExtraSource
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.bondExtra
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.delegatedStake.DelegatedStakeMigrationUseCase
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NominationPoolsBondMoreInteractor {

    suspend fun estimateFee(bondMoreAmount: Balance): Fee

    suspend fun bondMore(bondMoreAmount: Balance): Result<ExtrinsicSubmission>

    suspend fun stakeableAmount(asset: Asset): Balance
}

class RealNominationPoolsBondMoreInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
    private val migrationUseCase: DelegatedStakeMigrationUseCase,
    private val poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver,
) : NominationPoolsBondMoreInteractor {

    override suspend fun estimateFee(bondMoreAmount: Balance): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                bondExtra(bondMoreAmount)
            }
        }
    }

    override suspend fun bondMore(bondMoreAmount: Balance): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                bondExtra(bondMoreAmount)
            }
        }
    }

    override suspend fun stakeableAmount(asset: Asset): Balance {
        return poolsAvailableBalanceResolver.maximumBalanceToStake(asset)
    }

    private suspend fun ExtrinsicBuilder.bondExtra(amount: Balance) {
        migrationUseCase.migrateToDelegatedStakeIfNeeded()

        nominationPools.bondExtra(NominationPoolBondExtraSource.FreeBalance(amount))
    }
}
