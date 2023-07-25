package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.common.utils.combine as combineList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest


class StartStakingCompoundData(
    val chain: Chain,
    val asset: Asset,
    val availableBalance: BigInteger,
    val maxEarningRate: BigDecimal,
    val minStake: BigInteger,
    val eraInfo: StartStakingEraInfo,
    val participateInGovernance: Boolean,
    val participateInGovernanceMinAmount: BigInteger?,
    val payoutTypes: List<PayoutType>,
    val automaticPayoutMinAmount: BigInteger?
) {
}

interface CompoundStartStakingInteractor {

    fun observeStartStakingInfo(chain: Chain, chainAsset: Chain.Asset): Flow<StartStakingCompoundData>

}

class RealCompoundStartStakingInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val interactors: List<StartStakingInteractor>,
    private val stakingEraInteractor: StakingEraInteractor
) : CompoundStartStakingInteractor {

    override fun observeStartStakingInfo(chain: Chain, chainAsset: Chain.Asset): Flow<StartStakingCompoundData> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { metaId ->
                val asset = walletRepository.getAsset(metaId.id, chainAsset)

                val startStakingDataFlow = interactors.map { it.observeData(chain, asset!!) }
                    .combineList()

                val eraInfoData = stakingEraInteractor.observeEraInfo(chain)

                combine(startStakingDataFlow, eraInfoData) { startStakingData, startStakingEraInfo ->

                    val participationGovernanceMinAmount = startStakingData.filter { it.participationInGovernance }
                        .minOfOrNull { it.minStake }

                    val automaticPayoutMinAmount = startStakingData.filter { it.payoutType is PayoutType.Automatic }
                        .minOfOrNull { it.minStake }

                    StartStakingCompoundData(
                        chain = chain,
                        asset = asset!!,
                        availableBalance = startStakingData.map { it.availableBalance }.min(),
                        maxEarningRate = startStakingData.map { it.maxEarningRate }.max(),
                        minStake = startStakingData.map { it.minStake }.min(),
                        eraInfo = startStakingEraInfo,
                        participateInGovernance = startStakingData.map { it.participationInGovernance }.any { it },
                        participateInGovernanceMinAmount = participationGovernanceMinAmount,
                        payoutTypes = startStakingData.map { it.payoutType }.distinct(),
                        automaticPayoutMinAmount = automaticPayoutMinAmount
                    )
                }
            }
    }
}
