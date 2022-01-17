package io.novafoundation.nova.runtime.extrinsic

import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.SignedExtension
import java.math.BigInteger

object CustomSignedExtensions {

    private enum class CustomExtension(val extensionName: String, val typeName: String) {

        ASSETS_TX_PAYMENT("ChargeAssetTxPayment", "pallet_asset_tx_payment.ChargeAssetTxPayment") {

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

    fun create(runtime: RuntimeSnapshot): List<SignedExtension> {
        return CustomExtension.values().mapNotNull { constructSignedExtra(runtime, it) }
    }

    private fun constructSignedExtra(
        runtime: RuntimeSnapshot,
        customExtension: CustomExtension,
    ): SignedExtension? {
        return runtime.typeRegistry[customExtension.typeName]?.let { signedExtraType ->
            SignedExtension(customExtension.extensionName, signedExtraType, customExtension.createPayload())
        }
    }
}
