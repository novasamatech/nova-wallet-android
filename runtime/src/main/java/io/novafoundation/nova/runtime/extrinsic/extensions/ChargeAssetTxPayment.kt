package io.novafoundation.nova.runtime.extrinsic.extensions

import io.novafoundation.nova.common.utils.structOf
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.FixedValueTransactionExtension
import java.math.BigInteger

class ChargeAssetTxPayment(
    val assetId: Any? = null,
    val tip: BigInteger = BigInteger.ZERO
) : FixedValueTransactionExtension(
    name = ID,
    implicit = null,
    explicit = assetTxPaymentPayload(assetId, tip)
) {

    companion object {

        val ID = "ChargeAssetTxPayment"

        private fun assetTxPaymentPayload(assetId: Any?, tip: BigInteger = BigInteger.ZERO): Any {
            return structOf(
                "tip" to tip,
                "assetId" to assetId
            )
        }

        fun ExtrinsicBuilder.chargeAssetTxPayment(assetId: Any?, tip: BigInteger = BigInteger.ZERO) {
            setTransactionExtension(ChargeAssetTxPayment(assetId, tip))
        }
    }
}

