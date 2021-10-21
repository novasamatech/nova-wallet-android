package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks

class ExistentialDepositValidation<P, E>(
    private val totalBalanceProducer: AmountProducer<P>,
    private val feeProducer: AmountProducer<P>,
    private val extraAmountProducer: AmountProducer<P>,
    private val tokenProducer: TokenProducer<P>,
    private val errorProducer: () -> E,
    private val walletConstants: WalletConstants
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val token = tokenProducer(value)
        val existentialDepositInPlanks = walletConstants.existentialDeposit(token.configuration.chainId)
        val existentialDeposit = token.amountFromPlanks(existentialDepositInPlanks)

        val totalBalance = totalBalanceProducer(value)
        val fee = feeProducer(value)
        val extraAmount = extraAmountProducer(value)

        return validOrWarning(totalBalance - fee - extraAmount >= existentialDeposit, errorProducer)
    }
}
