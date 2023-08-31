package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractor
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.StartStakingEraInfo
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.validations.StartStakingLandingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.validations.startStalingLanding
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import io.novafoundation.nova.common.utils.combine as combineList

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
    val maxEarningRate: Perbill,
    val minStake: BigInteger,
    val eraInfo: StartStakingEraInfo,
    val participationInGovernance: ParticipationInGovernance,
    val payouts: Payouts
)

class LandingAvailableBalance(val asset: Asset, val availableBalance: BigInteger)

interface StakingTypeDetailsCompoundInteractor {

    val chain: Chain

    suspend fun validationSystem(): StartStakingLandingValidationSystem

    fun observeStartStakingInfo(): Flow<StartStakingCompoundData>

    fun observeAvailableBalance(): Flow<LandingAvailableBalance>
}

class RealStakingTypeDetailsCompoundInteractor(
    override val chain: Chain,
    private val chainAsset: Chain.Asset,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val interactors: List<StakingTypeDetailsInteractor>,
    private val stakingEraInteractor: StakingEraInteractor,
) : StakingTypeDetailsCompoundInteractor {

    override suspend fun validationSystem(): StartStakingLandingValidationSystem {
        return ValidationSystem.startStalingLanding()
    }

    override fun observeStartStakingInfo(): Flow<StartStakingCompoundData> {
        val startStakingDataFlow = interactors.map { it.observeData() }.combineList()
        val assetFlow = assetFlow()
        val eraInfoDataFlow = stakingEraInteractor.observeEraInfo()

        return combine(startStakingDataFlow, assetFlow, eraInfoDataFlow) { startStakingData, asset, eraInfo ->
            StartStakingCompoundData(
                chain = chain,
                asset = asset,
                maxEarningRate = startStakingData.maxOf { it.maxEarningRate },
                minStake = startStakingData.minOf { it.minStake },
                eraInfo = eraInfo,
                participationInGovernance = getParticipationInGovernance(startStakingData),
                payouts = getPayouts(startStakingData)
            )
        }
    }

    override fun observeAvailableBalance(): Flow<LandingAvailableBalance> {
        return assetFlow().map {
            val maxAvailableBalance = interactors
                .maxOfOrNull { interactor -> interactor.getAvailableBalance(it) }
                .orZero()

            LandingAvailableBalance(it, maxAvailableBalance)
        }
    }

    private fun assetFlow(): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { metaAccount -> walletRepository.assetFlow(metaAccount.id, chainAsset) }
    }

    private fun getParticipationInGovernance(stakingTypeDetails: List<StakingTypeDetails>): ParticipationInGovernance {
        val participationInGovernanceData = stakingTypeDetails.filter { it.participationInGovernance }

        return when {
            participationInGovernanceData.isNotEmpty() -> {
                val minAmount = participationInGovernanceData.minOf { it.minStake }
                val isParticipationInGovernanceHasSmallestMinStake = stakingTypeDetails.all { it.minStake >= minAmount }
                ParticipationInGovernance.Participate(minAmount, isParticipationInGovernanceHasSmallestMinStake)
            }
            else -> ParticipationInGovernance.NotParticipate
        }
    }

    private fun getPayouts(stakingTypeDetails: List<StakingTypeDetails>): Payouts {
        val automaticPayoutMinAmount = stakingTypeDetails.filter { it.payoutType is PayoutType.Automatically }
            .minOfOrNull { it.minStake }

        return Payouts(
            payoutTypes = stakingTypeDetails.map { it.payoutType }.distinct(),
            automaticPayoutMinAmount = automaticPayoutMinAmount,
            isAutomaticPayoutHasSmallestMinStake = isAutomaticPayoutHasSmallestMinStake(stakingTypeDetails, automaticPayoutMinAmount)
        )
    }

    private fun isAutomaticPayoutHasSmallestMinStake(stakingTypeDetails: List<StakingTypeDetails>, automaticPayoutMinAmount: BigInteger?): Boolean {
        if (automaticPayoutMinAmount == null) return false

        return stakingTypeDetails.all { it.minStake >= automaticPayoutMinAmount }
    }
}
