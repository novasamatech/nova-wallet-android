package io.novafoundation.nova.feature_staking_impl.domain.rewards

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.repository.ParasRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.VaraRepository
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
    private val varaRepository: VaraRepository,
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
            RELAYCHAIN, RELAYCHAIN_AURA -> {
                val custom = customRelayChainCalculator(validators, totalIssuance)
                if (custom != null) return custom

                val activePublicParachains = parasRepository.activePublicParachains(assetWithChain.chain.id)
                val inflationConfig = InflationConfig.create(chain.id, activePublicParachains)

                RewardCurveInflationRewardCalculator(validators, totalIssuance, inflationConfig)
            }

            ALEPH_ZERO -> AlephZeroRewardCalculator(validators, chainAsset = assetWithChain.asset)
            NOMINATION_POOLS, UNSUPPORTED, PARACHAIN, TURING -> throw IllegalStateException("Unknown staking type in RelaychainRewardFactory")
        }
    }

    private suspend fun StakingOption.customRelayChainCalculator(
        validators: List<RewardCalculationTarget>,
        totalIssuance: BigInteger
    ): RewardCalculator? {
        return when (chain.id) {
            Chain.Geneses.VARA -> Vara(chain.id, validators, totalIssuance)
            else -> null
        }
    }

    private fun InflationConfig.Companion.create(chainId: ChainId, activePublicParachains: Int?): InflationConfig {
        return when (chainId) {
            Chain.Geneses.POLKADOT -> Polkadot(activePublicParachains)
            Chain.Geneses.AVAIL_TURING_TESTNET, Chain.Geneses.AVAIL -> Avail()
            else -> Default(activePublicParachains)
        }
    }

    private suspend fun Vara(
        chainId: ChainId,
        validators: List<RewardCalculationTarget>,
        totalIssuance: BigInteger
    ): RewardCalculator? {
        return runCatching {
            val inflationInfo = varaRepository.getVaraInflation(chainId)

            VaraRewardCalculator(validators, totalIssuance, inflationInfo)
        }
            .onFailure {
                Log.e(LOG_TAG, "Failed to create Vara reward calculator, fallbacking to default", it)
            }
            .getOrNull()
    }
}
