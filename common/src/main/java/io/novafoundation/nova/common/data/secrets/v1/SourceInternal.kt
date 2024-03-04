package io.novafoundation.nova.common.data.secrets.v1

import io.novasama.substrate_sdk_android.scale.Schema
import io.novasama.substrate_sdk_android.scale.byteArray
import io.novasama.substrate_sdk_android.scale.string

internal enum class SourceType {
    CREATE, SEED, MNEMONIC, JSON, UNSPECIFIED
}

internal object SourceInternal : Schema<SourceInternal>() {
    val Type by string()

    val PrivateKey by byteArray()
    val PublicKey by byteArray()

    val Nonce by byteArray().optional()

    val Seed by byteArray().optional()
    val Mnemonic by string().optional()

    val DerivationPath by string().optional()
}
