package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getActiveElectedValidatorsExposures
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.error.accountIdNotFound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RewardCalculatorFactory(
    private val stakingRepository: StakingRepository,
    private val sharedState: StakingSharedState,
) {

    suspend fun create(
        exposures: AccountIdMap<Exposure>,
        validatorsPrefs: AccountIdMap<ValidatorPrefs?>
    ): RewardCalculator = withContext(Dispatchers.Default) {
        val chainId = sharedState.chainId()

        val totalIssuance = stakingRepository.getTotalIssuance(chainId)

        val validators = exposures.keys.mapNotNull { accountIdHex ->
            val exposure = exposures[accountIdHex] ?: accountIdNotFound(accountIdHex)
            val validatorPrefs = validatorsPrefs[accountIdHex] ?: return@mapNotNull null

            RewardCalculationTarget(
                accountIdHex = accountIdHex,
                totalStake = exposure.total,
                nominatorStakes = exposure.others,
                ownStake = exposure.own,
                commission = validatorPrefs.commission
            )
        }

        RewardCalculator(
            validators = validators,
            totalIssuance = totalIssuance
        )
    }

    suspend fun create(): RewardCalculator = withContext(Dispatchers.Default) {
        val chainId = sharedState.chainId()

        val exposures = stakingRepository.getActiveElectedValidatorsExposures(chainId)
        val validatorsPrefs = stakingRepository.getValidatorPrefs(chainId, exposures.keys.toList())

        create(exposures, validatorsPrefs)
    }
}
