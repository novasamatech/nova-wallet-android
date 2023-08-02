package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.common.utils.orZero
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
import kotlinx.coroutines.flow.map

sealed interface ParticipationInGovernance {
    class Participate(val minAmount: BigInteger?, val isParticipationInGovernanceHasSmallestMinStake: Boolean) : ParticipationInGovernance
    object NotParticipate : ParticipationInGovernance
}

class Payouts(
    val payoutTypes: List<PayoutType>,
    val automaticPayoutMinAmount: BigInteger?,
    val isAutomaticPayoutHasSmallestMinStake: Boolean
)

class StartStakingCompoundData(
    val chain: Chain,
    val asset: Asset,
    val maxEarningRate: BigDecimal,
    val minStake: BigInteger,
    val eraInfo: StartStakingEraInfo,
    val participationInGovernance: ParticipationInGovernance,
    val payouts: Payouts
)

class LandingAvailableBalance(val asset: Asset, val availableBalance: BigInteger)

interface CompoundStartStakingInteractor {

    fun observeStartStakingInfo(): Flow<StartStakingCompoundData>

    fun observeAvailableBalance(): Flow<LandingAvailableBalance>
}

class RealCompoundStartStakingInteractor(
    private val chain: Chain,
    private val chainAsset: Chain.Asset,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val interactors: List<StartStakingInteractor>,
    private val stakingEraInteractor: StakingEraInteractor
) : CompoundStartStakingInteractor {

    override fun observeStartStakingInfo(): Flow<StartStakingCompoundData> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { metaAccount -> walletRepository.assetFlow(metaAccount.id, chainAsset) }
            .flatMapLatest { asset ->
                val startStakingDataFlow = interactors.map { it.observeData(chain, asset) }
                    .combineList()

                val eraInfoData = stakingEraInteractor.observeEraInfo(chain)

                combine(startStakingDataFlow, eraInfoData) { startStakingData, startStakingEraInfo ->
                    StartStakingCompoundData(
                        chain = chain,
                        asset = asset,
                        maxEarningRate = startStakingData.map { it.maxEarningRate }.max(),
                        minStake = startStakingData.map { it.minStake }.min(),
                        eraInfo = startStakingEraInfo,
                        participationInGovernance = getParticipationInGovernance(startStakingData),
                        payouts = getPayouts(startStakingData)
                    )
                }
            }
    }

    override fun observeAvailableBalance(): Flow<LandingAvailableBalance> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { metaAccount -> walletRepository.assetFlow(metaAccount.id, chainAsset) }
            .map {
                val minAvailableBalance = interactors
                    .minOfOrNull { interactor -> interactor.getAvailableBalance(it) }
                    .orZero()

                LandingAvailableBalance(it, minAvailableBalance)
            }
    }

    private fun getParticipationInGovernance(startStakingData: List<StartStakingData>): ParticipationInGovernance {
        val participationInGovernanceData = startStakingData.filter { it.participationInGovernance }

        return when {
            participationInGovernanceData.isNotEmpty() -> {
                val minAmount = participationInGovernanceData.minOf { it.minStake }
                val isParticipationInGovernanceHasSmallestMinStake = startStakingData.all { it.minStake >= minAmount }
                ParticipationInGovernance.Participate(minAmount, isParticipationInGovernanceHasSmallestMinStake)
            }
            else -> ParticipationInGovernance.NotParticipate
        }
    }

    private fun getPayouts(startStakingData: List<StartStakingData>): Payouts {
        val automaticPayoutMinAmount = startStakingData.filter { it.payoutType is PayoutType.Automatic }
            .minOfOrNull { it.minStake }

        return Payouts(
            payoutTypes = startStakingData.map { it.payoutType }.distinct(),
            automaticPayoutMinAmount = automaticPayoutMinAmount,
            isAutomaticPayoutHasSmallestMinStake = isAutomaticPayoutHasSmallestMinStake(startStakingData, automaticPayoutMinAmount)
        )
    }

    private fun isAutomaticPayoutHasSmallestMinStake(startStakingData: List<StartStakingData>, automaticPayoutMinAmount: BigInteger?): Boolean {
        if (automaticPayoutMinAmount == null) return false

        return startStakingData.all { it.minStake >= automaticPayoutMinAmount }
    }
}
