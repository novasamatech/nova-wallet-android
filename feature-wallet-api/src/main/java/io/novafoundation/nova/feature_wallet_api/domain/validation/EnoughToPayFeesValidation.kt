package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import java.math.BigDecimal

class EnoughToPayFeesValidation<P, E>(
    private val feeExtractor: AmountProducer<P>,
    private val availableBalanceProducer: AmountProducer<P>,
    private val errorProducer: (P) -> E,
    private val extraAmountExtractor: AmountProducer<P> = { BigDecimal.ZERO },
) : Validation<P, E> {

    companion object;

    override suspend fun validate(value: P): ValidationStatus<E> {
        val fee = feeExtractor(value)
        val available = availableBalanceProducer(value)
        val amount = extraAmountExtractor(value)

        return if (fee + amount <= available) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, errorProducer(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.sufficientBalance(
    fee: AmountProducer<P> = { BigDecimal.ZERO },
    amount: AmountProducer<P> = { BigDecimal.ZERO },
    available: AmountProducer<P>,
    error: (P) -> E
) = validate(
    EnoughToPayFeesValidation(
        feeExtractor = fee,
        extraAmountExtractor = amount,
        errorProducer = error,
        availableBalanceProducer = available
    )
)

fun <P> EnoughToPayFeesValidation.Companion.assetBalanceProducer(
    walletRepository: WalletRepository,
    stakingSharedState: SingleAssetSharedState,
    originAddressExtractor: (P) -> String,
    chainAssetExtractor: (P) -> Chain.Asset,
): AmountProducer<P> = { payload ->
    val chain = stakingSharedState.chain()
    val accountId = chain.accountIdOf(originAddressExtractor(payload))

    val asset = walletRepository.getAsset(accountId, chainAssetExtractor(payload))!!

    asset.transferable
}
