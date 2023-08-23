package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.minStake
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.StakingMinimumBondError.ThresholdType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import java.math.RoundingMode
import kotlin.coroutines.coroutineContext

interface StakingMinimumBondError {

    class Context(val threshold: Balance, val chainAsset: Chain.Asset, val thresholdType: ThresholdType)

    enum class ThresholdType { REQUIRED, RECOMMENDED }

    val context: Context
}

class MinimumStakeValidation<P, E>(
    private val stakingRepository: StakingRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val chainAsset: (P) -> Chain.Asset,
    private val balanceToCheckAgainstRequired: suspend (P) -> Balance,
    private val balanceToCheckAgainstRecommended:suspend (P) -> Balance?,
    private val error: (StakingMinimumBondError.Context) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chainAsset = chainAsset(value)

        val scope = CoroutineScope(coroutineContext) // scope for cached execution == scope of coroutine

        val hardMinimum = stakingRepository.minimumNominatorBond(chainAsset.chainId)
        val recommendedMinimum = stakingSharedComputation.minStake(chainAsset.chainId, scope)

        val balanceToCheckAgainstRequired = balanceToCheckAgainstRequired(value)
        val balanceToCheckAgainstRecommended = balanceToCheckAgainstRecommended(value)

        return when {
            balanceToCheckAgainstRequired < hardMinimum -> {
                val context = StakingMinimumBondError.Context(hardMinimum, chainAsset, ThresholdType.REQUIRED)

                validationError(error(context))
            }

            balanceToCheckAgainstRecommended != null && balanceToCheckAgainstRecommended < recommendedMinimum -> {
                val context = StakingMinimumBondError.Context(recommendedMinimum, chainAsset, ThresholdType.RECOMMENDED)

                validationWarning(error(context))
            }

            else -> valid()
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.minimumBondValidation(
    stakingRepository: StakingRepository,
    stakingSharedComputation: StakingSharedComputation,
    chainAsset: (P) -> Chain.Asset,
    balanceToCheckAgainstRequired: suspend (P) -> Balance,
    balanceToCheckAgainstRecommended: suspend (P) -> Balance?,
    error: (StakingMinimumBondError.Context) -> E
) {
    validate(
        MinimumStakeValidation(
            stakingRepository = stakingRepository,
            stakingSharedComputation = stakingSharedComputation,
            chainAsset = chainAsset,
            balanceToCheckAgainstRequired = balanceToCheckAgainstRequired,
            balanceToCheckAgainstRecommended = balanceToCheckAgainstRecommended,
            error = error
        )
    )
}

fun handleStakingMinimumBondError(
    resourceManager: ResourceManager,
    error: StakingMinimumBondError
): TitleAndMessage {
    val title = resourceManager.getString(R.string.common_amount_low)

    val formattedThreshold = error.context.threshold.formatPlanks(error.context.chainAsset, RoundingMode.UP)
    val subtitle = when (error.context.thresholdType) {
        ThresholdType.REQUIRED -> resourceManager.getString(R.string.staking_setup_amount_too_low, formattedThreshold)
        ThresholdType.RECOMMENDED -> resourceManager.getString(R.string.staking_setup_amount_less_than_recommended, formattedThreshold)
    }

    return title to subtitle
}
