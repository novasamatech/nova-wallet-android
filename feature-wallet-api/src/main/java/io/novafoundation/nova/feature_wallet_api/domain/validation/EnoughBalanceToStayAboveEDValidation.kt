package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError.ErrorModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

interface InsufficientBalanceToStayAboveEDError {

    val asset: Chain.Asset
    val errorModel: ErrorModel

    class ErrorModel(
        val minRequiredBalance: BigDecimal,
        val availableBalance: BigDecimal,
        val balanceToAdd: BigDecimal
    )
}

class EnoughBalanceToStayAboveEDValidation<P, E, F : GenericFee>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val fee: GenericFeeProducer<F, P>,
    private val balance: AmountProducer<P>,
    private val chainWithAsset: (P) -> ChainWithAsset,
    private val error: (P, ErrorModel) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chainWithAsset(value).chain
        val asset = chainWithAsset(value).asset
        val existentialDeposit = assetSourceRegistry.existentialDeposit(chain, asset)
        val balance = balance(value)
        val fee = fee(value).networkFeeByRequestedAccountOrZero
        return validOrError(balance - fee >= existentialDeposit) {
            val minRequired = existentialDeposit + fee
            error(
                value,
                ErrorModel(
                    minRequiredBalance = minRequired,
                    availableBalance = balance,
                    balanceToAdd = minRequired - balance
                )
            )
        }
    }
}

class EnoughTotalToStayAboveEDValidationFactory(private val assetSourceRegistry: AssetSourceRegistry) {

    fun <P, E, F : GenericFee> create(
        fee: GenericFeeProducer<F, P>,
        balance: AmountProducer<P>,
        chainWithAsset: (P) -> ChainWithAsset,
        error: (P, ErrorModel) -> E
    ): EnoughBalanceToStayAboveEDValidation<P, E, F> {
        return EnoughBalanceToStayAboveEDValidation(
            assetSourceRegistry = assetSourceRegistry,
            fee = fee,
            balance = balance,
            chainWithAsset = chainWithAsset,
            error = error
        )
    }
}

context(ValidationSystemBuilder<P, E>)
fun <P, E, F : GenericFee> EnoughTotalToStayAboveEDValidationFactory.validate(
    fee: GenericFeeProducer<F, P>,
    balance: AmountProducer<P>,
    chainWithAsset: (P) -> ChainWithAsset,
    error: (P, ErrorModel) -> E
) {
    validate(create(fee, balance, chainWithAsset, error))
}
