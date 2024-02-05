package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.common.utils.toPerbill
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.repository.DockStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.ParasRepository
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.data.unwrapNominationPools
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.electedExposuresInActiveEra
import io.novafoundation.nova.feature_staking_impl.domain.error.accountIdNotFound
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.NOMINATION_POOLS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class RewardCalculatorFactory(
    private val stakingRepository: StakingRepository,
    private val totalIssuanceRepository: TotalIssuanceRepository,
    private val shareStakingSharedComputation: dagger.Lazy<StakingSharedComputation>,
    private val parasRepository: ParasRepository,
    private val dockStakingRepository: DockStakingRepository,
) {

    suspend fun create(
        stakingOption: StakingOption,
        exposures: AccountIdMap<Exposure>,
        validatorsPrefs: AccountIdMap<ValidatorPrefs?>
    ): RewardCalculator = withContext(Dispatchers.Default) {
        val totalIssuance = totalIssuanceRepository.getTotalIssuance(stakingOption.assetWithChain.chain.id)

        val validators = exposures.keys.mapNotNull { accountIdHex ->
            val exposure = exposures[accountIdHex] ?: accountIdNotFound(accountIdHex)
            val validatorPrefs = validatorsPrefs[accountIdHex] ?: return@mapNotNull null

            RewardCalculationTarget(
                accountIdHex = accountIdHex,
                totalStake = exposure.total,
                commission = validatorPrefs.commission
            )
        }

        stakingOption.createRewardCalculator(validators, totalIssuance)
    }

    suspend fun create(stakingOption: StakingOption, scope: CoroutineScope): RewardCalculator = withContext(Dispatchers.Default) {
        val chainId = stakingOption.assetWithChain.chain.id

        val exposures = shareStakingSharedComputation.get().electedExposuresInActiveEra(chainId, scope)
        val validatorsPrefs = stakingRepository.getValidatorPrefs(chainId, exposures.keys)

        create(stakingOption, exposures, validatorsPrefs)
    }

    private suspend fun StakingOption.createRewardCalculator(validators: List<RewardCalculationTarget>, totalIssuance: BigInteger): RewardCalculator {
        return when (unwrapNominationPools().stakingType) {
            RELAYCHAIN, RELAYCHAIN_AURA -> createRelaychainCalculator(validators, totalIssuance)
            ALEPH_ZERO -> AlephZeroRewardCalculator(validators, chainAsset = assetWithChain.asset)
            NOMINATION_POOLS, UNSUPPORTED, PARACHAIN, TURING -> throw IllegalStateException("Unknown staking type in RelaychainRewardFactory")
        }
    }

    private suspend fun StakingOption.createRelaychainCalculator(
        validators: List<RewardCalculationTarget>,
        totalIssuance: BigInteger
    ): RewardCalculator {
        return when (chain.id) {
            Chain.Geneses.DOCK -> createDockRewardCalculator(validators, totalIssuance)

            else -> {
                val activePublicParachains = parasRepository.activePublicParachains(assetWithChain.chain.id)
                val inflationConfig = InflationConfig.create(chain.id, activePublicParachains)

                RewardCurveInflationRewardCalculator(validators, totalIssuance, inflationConfig)
            }
        }
    }

    private suspend fun StakingOption.createDockRewardCalculator(
        validators: List<RewardCalculationTarget>,
        totalIssuance: BigInteger
    ): RewardCalculator {
        val totalStaked = validators.sumByBigInteger(RewardCalculationTarget::totalStake)

        val yearlyEmission = dockStakingRepository.getEarlyEmission(chain.id, totalStaked, totalIssuance)
        val treasuryRewardsPercentage = dockStakingRepository.getTreasuryRewardsPercentage(chain.id).toPerbill()

        return DockRewardCalculator(
            validators = validators,
            totalIssuance = totalIssuance,
            yearlyEmission = yearlyEmission,
            treasuryRewardsPercentage = treasuryRewardsPercentage
        )
    }

    private fun InflationConfig.Companion.create(chainId: ChainId, activePublicParachains: Int?): InflationConfig {
        return when (chainId) {
            Chain.Geneses.POLKADOT -> Polkadot(activePublicParachains)
            else -> Default(activePublicParachains)
        }
    }
}
