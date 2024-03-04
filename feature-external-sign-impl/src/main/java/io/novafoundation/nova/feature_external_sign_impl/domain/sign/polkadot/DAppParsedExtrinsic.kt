package io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import java.math.BigInteger

class DAppParsedExtrinsic(
    val address: String,
    val nonce: BigInteger,
    val specVersion: Int,
    val transactionVersion: Int,
    val genesisHash: ByteArray,
    val era: Era,
    val blockHash: ByteArray,
    val tip: BigInteger,
    val call: Extrinsic.EncodingInstance.CallRepresentation
)
