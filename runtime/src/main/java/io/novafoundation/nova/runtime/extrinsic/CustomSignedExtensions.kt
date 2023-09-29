package io.novafoundation.nova.runtime.extrinsic

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.metadata.SignedExtensionId
import java.math.BigInteger

object CustomSignedExtensions {

    private enum class CustomExtension(val extensionName: String) {

        ASSETS_TX_PAYMENT("ChargeAssetTxPayment") {

            override fun createPayload(): Any {
                return Struct.Instance(
                    mapOf(
                        "tip" to BigInteger.ZERO,
                        "assetId" to null
                    )
                )
            }
        };

        abstract fun createPayload(): Any?
    }

    fun extensionsWithValues(): Map<SignedExtensionId, Any?> {
        return CustomExtension.values().mapNotNull { customExtension ->
            customExtension.name to customExtension.createPayload()
        }.toMap()
    }
}
