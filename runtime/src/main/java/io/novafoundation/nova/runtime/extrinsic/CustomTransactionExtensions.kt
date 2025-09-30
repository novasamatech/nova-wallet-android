package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.runtime.extrinsic.extensions.ChargeAssetTxPayment
import io.novafoundation.nova.runtime.extrinsic.extensions.CheckAppId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.TransactionExtension

object CustomTransactionExtensions {

    fun applyDefaultValues(builder: ExtrinsicBuilder) {
        defaultValues().forEach(builder::setTransactionExtension)
    }

    fun defaultValues(): List<TransactionExtension> {
        return listOf(
            ChargeAssetTxPayment(),
            CheckAppId()
        )
    }
}
