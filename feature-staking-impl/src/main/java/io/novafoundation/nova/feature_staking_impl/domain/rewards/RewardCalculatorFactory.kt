package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getActiveElectedValidatorsExposures
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.common.repository.CommonStakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.error.accountIdNotFound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RewardCalculatorFactory(
    private val stakingRepository: StakingRepository,
    private val commonStakingRepository: CommonStakingRepository,
    @Deprecated("To be removed")
    private val sharedState: StakingSharedState,
) {

    suspend fun create(
        chainId: String,
        exposures: AccountIdMap<Exposure>,
        validatorsPrefs: AccountIdMap<ValidatorPrefs?>
    ): RewardCalculator = withContext(Dispatchers.Default) {
        val totalIssuance = commonStakingRepository.getTotalIssuance(chainId)

        val validators = exposures.keys.mapNotNull { accountIdHex ->
            val exposure = exposures[accountIdHex] ?: accountIdNotFound(accountIdHex)
            val validatorPrefs = validatorsPrefs[accountIdHex] ?: return@mapNotNull null

            RewardCalculationTarget(
                accountIdHex = accountIdHex,
                totalStake = exposure.total,
                commission = validatorPrefs.commission
            )
        }

        RewardCalculator(
            validators = validators,
            totalIssuance = totalIssuance
        )
    }

    @Deprecated(
        message = "Deprecated in favour of create(chainId: String)",
        replaceWith = ReplaceWith(expression = "create(chainId)")
    )
    suspend fun create(): RewardCalculator = create(sharedState.chainId())

    suspend fun create(chainId: String): RewardCalculator = withContext(Dispatchers.Default) {
        val exposures = stakingRepository.getActiveElectedValidatorsExposures(chainId)
        val validatorsPrefs = stakingRepository.getValidatorPrefs(chainId, exposures.keys.toList())

        create(chainId, exposures, validatorsPrefs)
    }
}
