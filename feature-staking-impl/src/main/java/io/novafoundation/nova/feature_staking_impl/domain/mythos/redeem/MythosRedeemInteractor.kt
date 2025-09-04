package io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.flattenDispatchFailure
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.release
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.totalRedeemable
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.getMythosLocks
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.total
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

interface MythosRedeemInteractor {

    fun redeemAmountFlow(): Flow<Balance>

    suspend fun estimateFee(): Fee

    suspend fun redeem(redeemAmount: Balance): Result<Pair<ExtrinsicExecutionResult, RedeemConsequences>>
}

@FeatureScope
class RealMythosRedeemInteractor @Inject constructor(
    private val userStakeRepository: MythosUserStakeRepository,
    private val chainStateRepository: ChainStateRepository,
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val balanceLocksRepository: BalanceLocksRepository,
) : MythosRedeemInteractor {

    override fun redeemAmountFlow(): Flow<Balance> {
        return flowOfAll {
            val chain = stakingSharedState.chain()
            val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain).intoKey()

            combine(
                userStakeRepository.releaseQueuesFlow(chain.id, accountId),
                chainStateRepository.currentBlockNumberFlow(chain.id)
            ) { releaseRequests, blockNumber ->
                releaseRequests.totalRedeemable(at = blockNumber)
            }
        }
    }

    override suspend fun estimateFee(): Fee {
        val chain = stakingSharedState.chain()

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            collatorStaking.release()
        }
    }

    override suspend fun redeem(redeemAmount: Balance): Result<Pair<ExtrinsicExecutionResult, RedeemConsequences>> {
        val (chain, chainAsset) = stakingSharedState.chainAndAsset()
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val mythStakingFreezes = balanceLocksRepository.getMythosLocks(metaAccount.id, chainAsset)

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) {
            collatorStaking.release()
        }
            .flattenDispatchFailure()
            .map {
                val redeemedAll = mythStakingFreezes.total == redeemAmount
                it to RedeemConsequences(willKillStash = redeemedAll)
            }
    }
}
