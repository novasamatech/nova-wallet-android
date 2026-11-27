package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.view.input.DecimalInputConstraints
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class QuoteProviderException(cause: Exception? = null) : Exception(cause) {

    class Unavailable(cause: Exception) : QuoteProviderException(cause)

    class AmountPrecisionExceeded(val constraints: DecimalInputConstraints.Bounded) : QuoteProviderException()

    sealed class DepositLimit(val asset: Chain.Asset) : QuoteProviderException() {

        class BelowMinimum(asset: Chain.Asset, val minimum: Balance) : DepositLimit(asset)

        class AboveMaximum(asset: Chain.Asset, val maximum: Balance?) : DepositLimit(asset)
    }
}
