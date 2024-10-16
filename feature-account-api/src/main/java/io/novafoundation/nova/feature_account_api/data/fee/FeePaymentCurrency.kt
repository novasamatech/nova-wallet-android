package io.novafoundation.nova.feature_account_api.data.fee

import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed interface FeePaymentCurrency {

    /**
     * Use native currency of the chain to pay the fee
     */
    object Native : FeePaymentCurrency

    /**
     * Request to use a specific [asset] for payment fees
     * This does not guarantee that the exact [asset] will be used for fee payments,
     * for example if the chain doesn't support paying fees in asset. In that case it will fall-back to using [FeePaymentCurrency.Native]
     *
     * The actual asset used to pay fees will be available in [Fee.asset]
     */
    class Asset(val asset: Chain.Asset) : FeePaymentCurrency {

        override fun equals(other: Any?): Boolean {
            if (other !is Asset) return false
            return asset.fullId == other.asset.fullId
        }

        override fun hashCode(): Int {
            return asset.hashCode()
        }
    }

    companion object
}

fun Chain.Asset.toFeePaymentCurrency(): FeePaymentCurrency {
    return when {
        isCommissionAsset -> FeePaymentCurrency.Native
        else -> FeePaymentCurrency.Asset(this)
    }
}
