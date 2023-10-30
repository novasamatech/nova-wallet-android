package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolAvailableBalanceValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.StakingMinimumBondError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class StartMultiStakingValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : StartMultiStakingValidationFailure(), NotEnoughToPayFeesError

    object NonPositiveAmount : StartMultiStakingValidationFailure()

    object NotEnoughAvailableToStake : StartMultiStakingValidationFailure()

    class AmountLessThanMinimum(override val context: StakingMinimumBondError.Context) : StartMultiStakingValidationFailure(), StakingMinimumBondError

    class MaxNominatorsReached(val stakingType: Chain.Asset.StakingType) : StartMultiStakingValidationFailure()

    class AvailableBalanceGap(
        val currentMaxAvailable: Balance,
        val alternativeMinStake: Balance,
        val biggestLockId: String,
        val chainAsset: Chain.Asset,
    ) : StartMultiStakingValidationFailure()

    class PoolAvailableBalance(
        override val context: PoolAvailableBalanceValidation.ValidationError.Context
    ) : PoolAvailableBalanceValidation.ValidationError, StartMultiStakingValidationFailure()

    object InactivePool : StartMultiStakingValidationFailure()
}
