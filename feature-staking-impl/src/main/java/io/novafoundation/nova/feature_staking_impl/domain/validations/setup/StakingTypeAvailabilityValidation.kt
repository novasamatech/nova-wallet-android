package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class StakingTypeAvailabilityValidation<P, E>(
    private val availableStakingTypes: List<Chain.Asset.StakingType>,
    private val stakingType: (P) -> Chain.Asset.StakingType,
    private val error: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return if (availableStakingTypes.contains(stakingType(value))) {
            valid()
        } else {
            validationError(error(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.stakingTypeAvailability(
    availableStakingTypes: List<Chain.Asset.StakingType>,
    stakingType: (P) -> Chain.Asset.StakingType,
    errorFormatter: (P) -> E,
) {
    validate(
        StakingTypeAvailabilityValidation(
            availableStakingTypes,
            stakingType,
            errorFormatter,
        )
    )
}
