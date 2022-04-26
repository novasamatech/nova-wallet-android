package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import java.math.BigDecimal
import java.math.BigInteger

sealed class ConfirmDAppOperationValidationFailure {

    object NotEnoughBalanceToPayFees : ConfirmDAppOperationValidationFailure()
}

class ConfirmDAppOperationValidationPayload(
    val token: Token?
)

inline fun ConfirmDAppOperationValidationPayload.convertingToAmount(planks: () -> BigInteger): BigDecimal {
    require(token != null) {
        "Invalid state - token should be present for validate transaction payload"
    }

    val feeInPlanks = planks()

    return token.amountFromPlanks(feeInPlanks)
}

typealias ConfirmDAppOperationValidationSystem = ValidationSystem<ConfirmDAppOperationValidationPayload, ConfirmDAppOperationValidationFailure>
