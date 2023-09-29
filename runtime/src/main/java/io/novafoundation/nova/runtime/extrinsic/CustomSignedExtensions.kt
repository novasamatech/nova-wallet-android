package io.novafoundation.nova.runtime.extrinsic

import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.create
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

    fun extensions(runtime: RuntimeSnapshot): List<SignedExtension> {
        return CustomExtension.values().mapNotNull { constructSignedExtra(runtime, it) }
    }

    fun extensionsWithValues(runtime: RuntimeSnapshot): Map<SignedExtension, Any?> {
        return CustomExtension.values().mapNotNull { customExtension ->
            constructSignedExtra(runtime, customExtension)?.let { signedExtension ->
                signedExtension to customExtension.createPayload()
            }
        }.toMap()
    }

    private fun constructSignedExtra(
        runtime: RuntimeSnapshot,
        customExtension: CustomExtension,
    ): SignedExtension? {
        return runtime.typeRegistry[customExtension.typeName]?.let { signedExtraType ->
            SignedExtension(customExtension.extensionName, signedExtraType)
        }
    }
}

fun Extrinsic.Companion.create(runtime: RuntimeSnapshot) = Extrinsic.create(CustomSignedExtensions.extensions(runtime))
