package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.repository.ParasRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.electedExposuresInActiveEra
import io.novafoundation.nova.feature_staking_impl.domain.error.accountIdNotFound
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
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
) {

    suspend fun create(
        chainAsset: Chain.Asset,
        exposures: AccountIdMap<Exposure>,
        validatorsPrefs: AccountIdMap<ValidatorPrefs?>
    ): RewardCalculator = withContext(Dispatchers.Default) {
        val totalIssuance = totalIssuanceRepository.getTotalIssuance(chainAsset.chainId)

        val validators = exposures.keys.mapNotNull { accountIdHex ->
            val exposure = exposures[accountIdHex] ?: accountIdNotFound(accountIdHex)
            val validatorPrefs = validatorsPrefs[accountIdHex] ?: return@mapNotNull null

            RewardCalculationTarget(
                accountIdHex = accountIdHex,
                totalStake = exposure.total,
                commission = validatorPrefs.commission
            )
        }

        chainAsset.createRewardCalculator(validators, totalIssuance)
    }

    suspend fun create(chainAsset: Chain.Asset, scope: CoroutineScope): RewardCalculator = withContext(Dispatchers.Default) {
        val chainId = chainAsset.chainId

        val exposures = shareStakingSharedComputation.get().electedExposuresInActiveEra(chainId, scope)
        val validatorsPrefs = stakingRepository.getValidatorPrefs(chainId, exposures.keys.toList())

        create(chainAsset, exposures, validatorsPrefs)
    }

    private suspend fun Chain.Asset.createRewardCalculator(validators: List<RewardCalculationTarget>, totalIssuance: BigInteger): RewardCalculator {
        // TODO staking dashboard - switch by selected staking option
        return when (staking.firstOrNull()) {
            RELAYCHAIN, RELAYCHAIN_AURA -> {
                val activePublicParachains = parasRepository.activePublicParachains(chainId)
                val inflationConfig = InflationConfig.Default(activePublicParachains)

                RewardCurveInflationRewardCalculator(validators, totalIssuance, inflationConfig)
            }
            ALEPH_ZERO -> AlephZeroRewardCalculator(validators, chainAsset = this)
            null, UNSUPPORTED, PARACHAIN, TURING -> throw IllegalStateException("Unknown staking type in RelaychainRewardFactory")
        }
    }
}
