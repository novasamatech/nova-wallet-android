package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions.CustomExtension.ASSETS_TX_PAYMENT
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signedExtra
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionId
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionValue
import java.math.BigInteger

object CustomSignedExtensions {

    private enum class CustomExtension(val extensionName: String) {

        ASSETS_TX_PAYMENT("ChargeAssetTxPayment") {

            override fun createPayload(): SignedExtensionValue {
                return SignedExtensionValue(
                    signedExtra = assetTxPaymentPayload(assetId = null)
                )
            }
        },

        // Signed extension for Avail related to Data Availability Transactions. We set it to 0 (provide)
        CHECK_APP_ID("CheckAppId") {

            override fun createPayload(): SignedExtensionValue {
                return SignedExtensionValue(
                    signedExtra = BigInteger.ZERO
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
