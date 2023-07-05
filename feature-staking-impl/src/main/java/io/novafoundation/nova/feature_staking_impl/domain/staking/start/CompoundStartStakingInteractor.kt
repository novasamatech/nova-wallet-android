package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.assetWithChain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Duration


class CompoundStartStakingInteractor(
    private val stakingSharedState: StakingSharedState,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val interactors: List<StartStakingInteractor>,
    private val stakingEraInteractor: StakingEraInteractor
) {

    fun supportedStakingTypes(): Flow<List<Chain.Asset.StakingType>> {
        return stakingSharedState.assetWithChain.map {
            it.asset.staking
        }
    }

    fun observeAvailableBalance(): Flow<BigInteger> {
        return combineToPair(accountRepository.selectedMetaAccountFlow(), stakingSharedState.assetWithChain)
            .flatMapLatest { (metaId, chainWithAsset) -> walletRepository.assetFlow(metaId.id, chainWithAsset.asset) }
            .map { it.freeInPlanks }
    }

    fun observeMaxEarningRate(): Flow<Double> {
        return interactors.map { it.observeMaxEarningRate() }
            .combine()
            .map { it.max() }
    }

    fun observeMinStake(): Flow<BigInteger> {
        return interactors.map { it.observeMinStake() }
            .combine()
            .map { it.min() }
    }

    fun observeRemainingEraDuration(): Flow<Duration> {
        return stakingEraInteractor.observeRemainingEraTime()
    }

    fun observeUnstakeDuration(): Flow<Duration> {
        return stakingEraInteractor.observeUnstakeTime()
    }

    fun observeEraDuration(): Flow<Duration> {
        return stakingEraInteractor.observeEraDuration()
    }

    fun observeParticipationInGovernance(): Flow<Boolean> {
        return stakingSharedState.assetWithChain
            .map { it.chain.governance.isNotEmpty() }
    }

    fun observePayoutTypes(): Flow<List<PayoutType>> {
        return interactors.map { it.observePayoutType() }
            .combine()
    }
}
