package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions.CustomExtension.ASSETS_TX_PAYMENT
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signedExtra
import jp.co.soramitsu.fearless_utils.runtime.metadata.SignedExtensionId
import jp.co.soramitsu.fearless_utils.runtime.metadata.SignedExtensionValue
import java.math.BigInteger

object CustomSignedExtensions {

    private enum class CustomExtension(val extensionName: String) {

        ASSETS_TX_PAYMENT("ChargeAssetTxPayment") {

            override fun createPayload(): SignedExtensionValue {
                return SignedExtensionValue(
                    signedExtra = assetTxPaymentPayload(assetId = null)
                )
            }
        };

        abstract fun createPayload(): SignedExtensionValue
    }

    fun ExtrinsicBuilder.assetTxPayment(assetId: Any?, tip: BigInteger = BigInteger.ZERO) {
        val extensionValue = assetTxPaymentPayload(assetId, tip)
        signedExtra(ASSETS_TX_PAYMENT.extensionName, extensionValue)
    }

    fun extensionsWithValues(): Map<SignedExtensionId, SignedExtensionValue> {
        return CustomExtension.values().mapNotNull { customExtension ->
            customExtension.extensionName to customExtension.createPayload()
        }.toMap()
    }

    private fun assetTxPaymentPayload(assetId: Any?, tip: BigInteger = BigInteger.ZERO): Any {
        return structOf(
            "tip" to tip,
            "assetId" to assetId
        )
    }
}
