package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.StakingMinimumBondError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class StartMultiStakingValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val availableToPayFees: BigDecimal,
        override val fee: BigDecimal
    ) : StartMultiStakingValidationFailure(), NotEnoughToPayFeesError

    object NonPositiveAmount : StartMultiStakingValidationFailure()

    object NotEnoughAvailableToStake : StartMultiStakingValidationFailure()

    class AmountLessThanMinimum(override val context: StakingMinimumBondError.Context) : StartMultiStakingValidationFailure(), StakingMinimumBondError

    class MaxNominatorsReached(val stakingType: Chain.Asset.StakingType) : StartMultiStakingValidationFailure()
}
