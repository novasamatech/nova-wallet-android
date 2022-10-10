package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getActiveElectedValidatorsExposures
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.error.accountIdNotFound
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class RewardCalculatorFactory(
    private val stakingRepository: StakingRepository,
    private val totalIssuanceRepository: TotalIssuanceRepository,
    @Deprecated("To be removed")
    private val sharedState: StakingSharedState,
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

    @Deprecated(
        message = "Deprecated in favour of create(chainId: String)"
    )
    suspend fun create(): RewardCalculator = create(sharedState.chainAsset())

    suspend fun create(chainAsset: Chain.Asset): RewardCalculator = withContext(Dispatchers.Default) {
        val chainId = chainAsset.chainId

        val exposures = stakingRepository.getActiveElectedValidatorsExposures(chainId)
        val validatorsPrefs = stakingRepository.getValidatorPrefs(chainId, exposures.keys.toList())

        create(chainAsset, exposures, validatorsPrefs)
    }

    private fun Chain.Asset.createRewardCalculator(validators: List<RewardCalculationTarget>, totalIssuance: BigInteger): RewardCalculator {
        return when (staking) {
            RELAYCHAIN, RELAYCHAIN_AURA -> RewardCurveInflationRewardCalculator(validators, totalIssuance)
            ALEPH_ZERO -> AlephZeroRewardCalculator(validators, totalIssuance, chainAsset = this)
            UNSUPPORTED, PARACHAIN, TURING -> throw IllegalStateException("Unknown staking type in RelaychainRewardFactory")
        }
    }
}
