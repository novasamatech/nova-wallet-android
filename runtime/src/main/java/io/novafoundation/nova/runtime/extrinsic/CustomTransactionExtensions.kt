package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.runtime.extrinsic.extensions.ChargeAssetTxPayment
import io.novafoundation.nova.runtime.extrinsic.extensions.CheckAppId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.TransactionExtension

object CustomTransactionExtensions {

    context(ExtrinsicBuilder)
    fun applyDefaultValues() {
        defaultValues().forEach(::setTransactionExtension)
    }

    fun defaultValues(): List<TransactionExtension> {
        return listOf(
            ChargeAssetTxPayment(),
            CheckAppId()
        )
    }
}
