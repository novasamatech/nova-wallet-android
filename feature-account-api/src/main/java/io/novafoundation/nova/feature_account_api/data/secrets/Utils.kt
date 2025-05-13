package io.novafoundation.nova.feature_account_api.data.secrets

import io.novafoundation.nova.feature_account_api.data.derivationPath.DerivationPathDecoder
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory

fun SubstrateKeypairFactory.generateSr25119Keypair(seed: ByteArray, derivationPath: String): Sr25519Keypair {
    val decodedDerivationPath = DerivationPathDecoder.decodeSubstrateDerivationPath(derivationPath) ?: error("Derivation path are not decodable")
    val junctions = decodedDerivationPath.junctions
    return generate(EncryptionType.SR25519, seed, junctions) as Sr25519Keypair
}
