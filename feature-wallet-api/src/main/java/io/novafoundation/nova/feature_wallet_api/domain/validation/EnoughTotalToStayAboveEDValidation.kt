package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface InsufficientTotalToStayAboveEDError {
    val asset: Chain.Asset
}

class EnoughTotalToStayAboveEDValidation<P, E>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val fee: AmountProducer<P>,
    private val totalBalance: AmountProducer<P>,
    private val chainWithAsset: (P) -> ChainWithAsset,
    private val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chainWithAsset(value).chain
        val asset = chainWithAsset(value).asset
        val existentialDeposit = assetSourceRegistry.existentialDeposit(chain, asset)
        return validOrError(totalBalance(value) - fee(value) >= existentialDeposit) {
            error(value)
        }
    }
}

class EnoughTotalToStayAboveEDValidationFactory(private val assetSourceRegistry: AssetSourceRegistry) {

    fun <P, E> create(
        fee: AmountProducer<P>,
        total: AmountProducer<P>,
        chainWithAsset: (P) -> ChainWithAsset,
        error: (P) -> E
    ): EnoughTotalToStayAboveEDValidation<P, E> {
        return EnoughTotalToStayAboveEDValidation(
            assetSourceRegistry = assetSourceRegistry,
            fee = fee,
            totalBalance = total,
            chainWithAsset = chainWithAsset,
            error = error
        )
    }
}

context(ValidationSystemBuilder<P, E>)
fun <P, E> EnoughTotalToStayAboveEDValidationFactory.validate(
    fee: AmountProducer<P>,
    total: AmountProducer<P>,
    chainWithAsset: (P) -> ChainWithAsset,
    error: (P) -> E
) {
    validate(create(fee, total, chainWithAsset, error))
}
