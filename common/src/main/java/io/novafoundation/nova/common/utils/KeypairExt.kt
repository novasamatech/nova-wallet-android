package io.novafoundation.nova.common.utils

import io.novafoundation.nova.common.data.secrets.v2.KeyPairSchema
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.scale.EncodableStruct

fun Keypair.toStruct(): EncodableStruct<KeyPairSchema> {
    return KeyPairSchema { keypair ->
        keypair[PublicKey] = publicKey
        keypair[PrivateKey] = privateKey
        keypair[Nonce] = (this@toStruct as? Sr25519Keypair)?.nonce
    }
}
