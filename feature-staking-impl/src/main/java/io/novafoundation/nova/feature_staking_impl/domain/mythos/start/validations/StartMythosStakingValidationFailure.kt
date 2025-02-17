package io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class StartMythosStakingValidationFailure {

    object NotPositiveAmount : StartMythosStakingValidationFailure()

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : StartMythosStakingValidationFailure(), NotEnoughToPayFeesError

    object NotEnoughStakeableBalance : StartMythosStakingValidationFailure()

    class TooLowStakeAmount(
        val minimumStake: Balance,
        val asset: Asset
    ) : StartMythosStakingValidationFailure()
}
