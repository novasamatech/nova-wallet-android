package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class YieldBoostValidationFailure {

    class FirstTaskCannotExecute(
        val chainAsset: Chain.Asset,
        val minimumBalanceRequired: BigDecimal,
        val networkFee: BigDecimal,
        val availableBalanceBeforeFees: BigDecimal,
        val type: Type
    ) : YieldBoostValidationFailure() {

        enum class Type {
            EXECUTION_FEE, THRESHOLD
        }
    }

    class WillCancelAllExistingTasks(
        val newCollator: Collator,
    ) : YieldBoostValidationFailure()

    class NotEnoughToPayToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : YieldBoostValidationFailure(), NotEnoughToPayFeesError
}
